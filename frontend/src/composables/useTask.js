import { reactive, onBeforeUnmount } from 'vue'
import { taskApi } from '@/api/tasks'
import { toast } from '@/utils/toast'

/**
 * 追蹤後端背景工作（下載、安裝、備份…）的進度。
 *   const { state, track } = useTaskRunner()
 *   post(..., (d) => track(d.taskId, { title: '下載中', onDone: reload }))
 * state：{ active, title, percent(-1=不確定), message }
 */
export function useTaskRunner() {
  const state = reactive({ active: false, title: '', percent: -1, message: '' })
  let timer = null
  let gone = false

  function track(taskId, { title = '', onDone, onFail } = {}) {
    Object.assign(state, { active: true, title, percent: -1, message: '' })
    let fails = 0
    const tick = () => {
      taskApi.get(
        taskId,
        (t) => {
          fails = 0
          state.percent = t.percent
          state.message = t.message || ''
          if (t.status === 'RUNNING') {
            if (!gone) timer = setTimeout(tick, 900)
            return
          }
          state.active = false
          if (t.status === 'SUCCESS') onDone?.(t)
          else {
            toast.error(t.error || '工作失敗')
            onFail?.(t)
          }
        },
        {
          showError: false,
          onError: (message) => {
            // 暫時連不上就重試幾次；工作本身仍在後端繼續執行
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

  onBeforeUnmount(() => {
    gone = true
    clearTimeout(timer)
  })
  return { state, track }
}
