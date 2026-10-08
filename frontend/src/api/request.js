import http from './http'
import { toast } from '@/utils/toast'

export const MSG_NETWORK = '伺服器請求失敗，請稍後再試'
export const MSG_TIMEOUT = '伺服器回應逾時，請稍後再試'

// 401 時要做什麼（登出並回登入頁）由 main.js 註冊，避免循環相依
let unauthorizedHandler = null
export const setUnauthorizedHandler = (fn) => { unauthorizedHandler = fn }
// 非 axios 的請求（例如 SSE 的 fetch）收到 401 時，也走同一套登出流程
export const notifyUnauthorized = () => unauthorizedHandler?.()

/** 把各種失敗情境統一轉成後端格式：{ status:0, message } */
function normalizeError(err) {
  const res = err.response
  if (!res) {
    // 沒收到回應：後端掛掉、網路斷線、逾時、CORS 被擋
    return { status: 0, message: err.code === 'ECONNABORTED' ? MSG_TIMEOUT : MSG_NETWORK }
  }
  if (res.status === 401 && unauthorizedHandler) unauthorizedHandler()
  const body = res.data
  if (body && typeof body === 'object' && 'status' in body) return body
  // 有回應但不是我們的格式（例如 Nginx 502/504、後端崩潰的 HTML 頁）
  return { status: 0, message: `${MSG_NETWORK}（${res.status}）` }
}

/**
 * 共用請求方法
 * @param {'get'|'post'} method
 * @param {string} url
 * @param {object} reqBody  post 為 body；get 為 query 參數
 * @param {(detail:any, body:object)=>void} callBack  status=1 時執行，參數為後端的 detail
 * @param {object} options
 *   showSuccess  成功時是否顯示後端 message（預設 false）
 *   showError    失敗時是否自動顯示錯誤訊息（預設 true）
 *   onError      失敗時的額外處理 (message, body) => void
 *   onFinally    不論成功失敗都會執行（常用來關閉 loading）
 *   axios        額外的 axios 設定（例如 { timeout: 0, onUploadProgress }）
 */
async function request(method, url, reqBody, callBack, options = {}) {
  const { showSuccess = false, showError = true, onError, onFinally, axios: axiosConfig = {} } = options

  let body
  try {
    const res = await http.request({
      method,
      url,
      ...(method === 'get' ? { params: reqBody } : { data: reqBody }),
      ...axiosConfig
    })
    body = res.data
  } catch (err) {
    body = normalizeError(err)
  }

  try {
    if (body && body.status === 1) {
      if (showSuccess && body.message) toast.success(body.message)
      await callBack?.(body.detail, body)
    } else {
      const message = body?.message || '操作失敗'
      if (showError) toast.error(message)
      onError?.(message, body)
    }
  } finally {
    onFinally?.()
  }
  return body
}

export const post = (url, reqBody, callBack, options) => request('post', url, reqBody, callBack, options)
export const get = (url, reqBody, callBack, options) => request('get', url, reqBody, callBack, options)

/**
 * 上傳檔案：請求本體就是檔案內容（後端以串流寫入磁碟，大檔也不會佔用記憶體）。
 * 檔名等參數請放在 url 的 query。逾時關閉；options.onProgress(percent) 取得上傳進度。
 */
export const upload = (url, file, callBack, options = {}) => {
  const { onProgress, ...rest } = options
  return request('post', url, file, callBack, {
    ...rest,
    axios: {
      timeout: 0,
      maxBodyLength: Infinity,
      maxContentLength: Infinity,
      headers: { 'Content-Type': 'application/octet-stream' },
      onUploadProgress: onProgress && ((e) => e.total && onProgress(Math.round((e.loaded * 100) / e.total)))
    }
  })
}
