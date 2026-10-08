import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'
import { authApi } from '@/api/auth'

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
          storage.setToken(detail.token)
          storage.setUser(detail.user)
          onSuccess?.()
        },
        { showSuccess: true, onFinally }
      )
    },
    logout() {
      this.token = ''
      this.user = null
      storage.clear()
    }
  }
})
