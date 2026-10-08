import { post, get } from './request'

// 每個後端模組一個檔案，這裡只做「url 對應」，其餘交給共用 request
export const authApi = {
  login: (body, cb, opt) => post('/api/auth/login', body, cb, opt),
  register: (body, cb, opt) => post('/api/auth/register', body, cb, opt),
  registerEnabled: (cb, opt) => get('/api/auth/register-enabled', null, cb, opt),
  me: (cb, opt) => get('/api/auth/me', null, cb, opt)
}
