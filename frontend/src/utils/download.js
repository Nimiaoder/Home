/**
 * 以後端發的一次性票券觸發瀏覽器下載（大檔由瀏覽器直接串流，不佔用前端記憶體）。
 * @param {{ticket:string,fileName:string}} t
 */
export function downloadByTicket(t) {
  const base = import.meta.env.VITE_API_BASE || ''
  const a = document.createElement('a')
  a.href = `${base}/api/dl/${t.ticket}`
  a.download = t.fileName || ''
  document.body.appendChild(a)
  a.click()
  a.remove()
}
