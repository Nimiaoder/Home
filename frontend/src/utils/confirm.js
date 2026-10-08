import { reactive } from 'vue'

// 全域確認對話框：任何地方 import { confirmDialog } 就能用，回傳 Promise<{ ok, checked }>
export const confirmState = reactive({ open: false, options: {}, resolve: null, checked: false })

/**
 * @param {{title?:string, message?:string, confirmText?:string, danger?:boolean,
 *          checkbox?:string, checkboxDefault?:boolean}} options
 */
export function confirmDialog(options = {}) {
  return new Promise((resolve) => {
    confirmState.options = options
    confirmState.checked = !!options.checkboxDefault
    confirmState.resolve = resolve
    confirmState.open = true
  })
}

export function settleConfirm(ok) {
  const resolve = confirmState.resolve
  confirmState.open = false
  confirmState.resolve = null
  resolve?.({ ok, checked: confirmState.checked })
}
