import { storage } from '@/utils/storage'
import { notifyUnauthorized } from './request'

/**
 * 以 fetch 讀取 Server-Sent Events（EventSource 無法帶 Authorization 標頭，所以自己解析）。
 * 連線結束（正常或中斷）時 Promise 結束；被 signal 中止時會丟出 AbortError。
 * @param {string} url  例如 /api/minecraft/servers/1/console/stream?after=0
 * @param {{signal?:AbortSignal, onEvent:(e:{name:string,data:string})=>void}} opts
 */
export async function streamSse(url, { signal, onEvent }) {
  const base = import.meta.env.VITE_API_BASE || ''
  const res = await fetch(base + url, {
    headers: { Authorization: `Bearer ${storage.getToken()}`, Accept: 'text/event-stream' },
    signal
  })
  if (res.status === 401) {
    notifyUnauthorized()
    throw new Error('unauthorized')
  }
  if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`)

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    let idx
    while ((idx = buf.indexOf('\n\n')) >= 0) {
      const block = buf.slice(0, idx)
      buf = buf.slice(idx + 2)
      const evt = parseBlock(block)
      if (evt) onEvent(evt)
    }
  }
}

function parseBlock(block) {
  let name = 'message'
  const data = []
  for (const line of block.split('\n')) {
    if (!line || line.startsWith(':')) continue
    const i = line.indexOf(':')
    const field = i < 0 ? line : line.slice(0, i)
    const val = i < 0 ? '' : line.slice(i + 1).replace(/^ /, '')
    if (field === 'event') name = val
    else if (field === 'data') data.push(val)
  }
  return data.length || name !== 'message' ? { name, data: data.join('\n') } : null
}
