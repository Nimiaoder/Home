import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'
import { authApi } from '@/api/auth'
import { logger } from '@/utils/logger'

const log = logger('auth')

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: storage.getToken(),
    user: storage.getUser()
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    /** 登入成功才會執行 onSuccess；失敗訊息由共用 request 自動顯示 */
    login(username, password, { onSuccess, onFinally } = {}) {
      return authApi.login(
        { username, password },
        (detail) => {
          this.token = detail.token
          this.user = detail.user
          log.info('登入成功', detail.user?.username, detail.user?.role)
          storage.setToken(detail.token)
          storage.setUser(detail.user)
          onSuccess?.()
        },
        { showSuccess: true, onFinally }
      )
    },
    logout() {
      log.info('清除登入狀態')
      this.token = ''
      this.user = null
      storage.clear()
    }
  }
})
