import { get } from './request'

// 背景工作的分類，要和後端 TaskService.submit 的 scope 一致。
// 重新整理頁面後，就是靠它向後端找回還在進行中的工作。
export const taskScope = {
  java: 'mc:java',
  library: 'mc:library',
  mods: (serverId) => `mc:mods:${serverId}`,
  worlds: (serverId) => `mc:worlds:${serverId}`
}

export const taskApi = {
  get: (id, cb, opt) => get(`/api/tasks/${id}`, null, cb, opt),
  active: (scope, cb, opt) => get('/api/tasks/active', { scope }, cb, opt)
}
