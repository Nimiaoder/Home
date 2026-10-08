import { post, get } from './request'

// 每個後端模組一個檔案，這裡只做「url 對應」，其餘交給共用 request
export const authApi = {
  login: (body, cb, opt) => post('/api/auth/login', body, cb, opt),
  me: (cb, opt) => get('/api/auth/me', null, cb, opt)
}

export const demoApi = {
  hello: (cb, opt) => get('/api/demo/hello', null, cb, opt),
  admin: (cb, opt) => get('/api/demo/admin', null, cb, opt)
}
