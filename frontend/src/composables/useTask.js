import { reactive, onBeforeUnmount } from 'vue'
import { taskApi } from '@/api/tasks'
import { logger } from '@/utils/logger'
import { toast } from '@/utils/toast'

const log = logger('useTask')

/**
 * 追蹤後端背景工作（下載、安裝、備份…）的進度。
 *   const { state, track, resume } = useTaskRunner()
 *   post(..., (d) => track(d.taskId, { title: '下載中', onDone: reload }))
 *   onMounted(() => resume(taskScope.java, { onDone: reload }))   // 重新整理後接回進行中的工作
 * state：{ active, title, percent(-1=不確定), message, bytesDone, bytesTotal(-1=未知), speed(bytes/秒) }
 */
export function useTaskRunner() {
  const state = reactive({ active: false, title: '', percent: -1, message: '', bytesDone: 0, bytesTotal: -1, speed: 0 })
  let timer = null
  let gone = false

  function apply(t) {
    state.percent = t.percent
    state.message = t.message || ''
    state.bytesDone = t.bytesDone || 0
    state.bytesTotal = t.bytesTotal ?? -1
    state.speed = t.speed || 0
  }

  function track(taskId, { title = '', onDone, onFail } = {}) {
    clearTimeout(timer)
    log.info('開始追蹤工作', taskId, title)
    Object.assign(state, { active: true, title, percent: -1, message: '', bytesDone: 0, bytesTotal: -1, speed: 0 })
    let fails = 0
    const tick = () => {
      taskApi.get(
        taskId,
        (t) => {
          fails = 0
          apply(t)
          if (t.status === 'RUNNING') {
            if (!gone) timer = setTimeout(tick, 900)
            return
          }
          state.active = false
          if (t.status === 'SUCCESS') {
            log.info('工作完成', taskId, title)
            onDone?.(t)
          } else {
            log.warn('工作失敗', taskId, t.error)
            toast.error(t.error || '工作失敗')
            onFail?.(t)
          }
        },
        {
          showError: false,
          onError: (message) => {
            // 暫時連不上就重試幾次；工作本身仍在後端繼續執行
            log.warn('查詢工作進度失敗', taskId, message, `（第 ${fails + 1} 次）`)
            if (++fails <= 5 && !gone) timer = setTimeout(tick, 2000)
            else {
              state.active = false
              toast.error(message)
            }
          }
        }
      )
    }
    tick()
  }

  /** 向後端找這個 scope 底下還在跑的工作，有的話接回去顯示進度。 */
  function resume(scope, opts = {}) {
    if (state.active) return
    taskApi.active(scope, (list) => {
      const t = list?.[0]
      if (!t || gone) return
      log.info('找回進行中的工作', scope, t.id, t.title)
      track(t.id, { ...opts, title: opts.title || t.title })
    }, { showError: false })
  }

  onBeforeUnmount(() => {
    gone = true
    clearTimeout(timer)
  })
  return { state, track, resume }
}
