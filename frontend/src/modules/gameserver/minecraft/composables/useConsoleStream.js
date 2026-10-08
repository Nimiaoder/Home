import { ref, shallowRef, onBeforeUnmount } from 'vue'
import { streamSse } from '@/api/stream'
import { mcServerApi } from '@/api/minecraft'

const MAX_LINES = 3000

/**
 * 訂閱某個伺服器的主控台串流（SSE），自動重連並從上次收到的位置續傳。
 * 回傳：lines（shallowRef，每個元素 {seq,text}）、status（最新執行狀態）、connected、fatal（致命錯誤訊息）、clear()
 */
export function useConsoleStream(serverId) {
  const lines = shallowRef([])
  const status = ref(null)
  const connected = ref(false)
  const fatal = ref('')

  let ctrl = null
  let stopped = false
  let lastSeq = 0
  let epoch = 0
  let pending = []
  let flushScheduled = false

  // 一次收到大量行（例如啟動時）時，合併到下一個動畫影格再更新畫面
  function flush() {
    flushScheduled = false
    if (!pending.length) return
    const merged = lines.value.concat(pending)
    pending = []
    lines.value = merged.length > MAX_LINES ? merged.slice(merged.length - MAX_LINES) : merged
  }

  function onEvent({ name, data }) {
    if (name === 'hello') {
      const h = JSON.parse(data)
      epoch = h.epoch
      if (h.reset) {
        lines.value = []
        pending = []
        lastSeq = 0
      }
      connected.value = true
    } else if (name === 'lines') {
      const batch = JSON.parse(data)
      if (!batch.length) return
      lastSeq = batch[batch.length - 1].seq
      pending.push(...batch)
      if (!flushScheduled) {
        flushScheduled = true
        requestAnimationFrame(flush)
      }
    } else if (name === 'status') {
      status.value = JSON.parse(data)
    } else if (name === 'fatal') {
      fatal.value = data
    }
  }

  const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

  async function loop() {
    while (!stopped && !fatal.value) {
      ctrl = new AbortController()
      try {
        await streamSse(mcServerApi.consoleStreamUrl(serverId(), lastSeq, epoch), { signal: ctrl.signal, onEvent })
      } catch {
        // 中斷或網路錯誤：稍後重連
      }
      connected.value = false
      if (!stopped && !fatal.value) await sleep(2000)
    }
  }

  loop()
  onBeforeUnmount(() => {
    stopped = true
    ctrl?.abort()
  })

  return {
    lines, status, connected, fatal,
    clear() {
      pending = []
      lines.value = []
    }
  }
}
