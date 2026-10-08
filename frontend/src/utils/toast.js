import { reactive } from 'vue'

// 全域提示訊息：任何地方 import { toast } 就能用
export const toastState = reactive({ list: [] })
let seq = 0

function push(type, message, duration = 3200) {
  if (!message) return
  const id = ++seq
  toastState.list.push({ id, type, message })
  setTimeout(() => remove(id), duration)
}

export function remove(id) {
  const i = toastState.list.findIndex((t) => t.id === id)
  if (i > -1) toastState.list.splice(i, 1)
}

export const toast = {
  success: (m) => push('success', m),
  error: (m) => push('error', m, 4500),
  info: (m) => push('info', m)
}
