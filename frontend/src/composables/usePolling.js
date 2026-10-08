import { onMounted, onBeforeUnmount } from 'vue'

/** 掛載後立即執行一次，之後每 intervalMs 毫秒重複；頁面隱藏時暫停。 */
export function usePolling(fn, intervalMs) {
  let timer = null
  const run = () => { if (!document.hidden) fn() }
  onMounted(() => {
    fn()
    timer = setInterval(run, intervalMs)
  })
  onBeforeUnmount(() => clearInterval(timer))
}
