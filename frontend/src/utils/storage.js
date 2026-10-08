// 集中管理 localStorage，之後要改成 sessionStorage 或加密只需改這裡
const TOKEN_KEY = 'dev_token'
const USER_KEY = 'dev_user'

export const storage = {
  getToken: () => localStorage.getItem(TOKEN_KEY) || '',
  setToken: (t) => localStorage.setItem(TOKEN_KEY, t),
  getUser: () => {
    try { return JSON.parse(localStorage.getItem(USER_KEY)) } catch { return null }
  },
  setUser: (u) => localStorage.setItem(USER_KEY, JSON.stringify(u)),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}
