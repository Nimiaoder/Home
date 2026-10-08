import { inject, provide } from 'vue'

const KEY = Symbol('mcServerContext')

/** McServerLayout 提供；各分頁（總覽、主控台…）用 useServer() 取得同一份資料。 */
export function provideServer(ctx) {
  provide(KEY, ctx)
}

/** @returns {{ server: import('vue').Ref<any>, reload: () => void, id: import('vue').ComputedRef<number> }} */
export function useServer() {
  const ctx = inject(KEY)
  if (!ctx) throw new Error('useServer() 只能在 McServerLayout 底下使用')
  return ctx
}
