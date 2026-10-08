import { ref } from 'vue'
import { mcServerApi } from '@/api/minecraft'
import { confirmDialog } from '@/utils/confirm'
import { logger } from '@/utils/logger'

const log = logger('usePower')

const CONFIRMS = {
  stop: { title: '停止伺服器', message: '會先通知玩家並儲存地圖，再正常關閉。', confirmText: '停止' },
  restart: { title: '重新啟動', message: '伺服器會先正常關閉，再自動啟動。', confirmText: '重新啟動' },
  kill: { title: '強制結束', message: '直接終止 Java 行程，可能遺失最近尚未存檔的進度。\n只有在無法正常停止時才使用。', confirmText: '強制結束', danger: true }
}

/**
 * 電源操作（start / stop / restart / kill），含確認對話框。
 * @param {() => any} getServer  回傳目前伺服器
 * @param {() => void} onDone    操作送出成功後（通常是重新整理）
 */
export function usePower(getServer, onDone) {
  const busy = ref(false)

  async function act(action) {
    const s = getServer()
    if (!s) return
    const conf = CONFIRMS[action]
    if (conf) {
      const { ok } = await confirmDialog(conf)
      if (!ok) return
    }
    log.info('電源操作', action, s.name)
    busy.value = true
    mcServerApi.power(s.id, action, () => onDone?.(), { onFinally: () => (busy.value = false) })
  }
  return { busy, act }
}
