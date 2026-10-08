import { get } from './request'

export const taskApi = {
  get: (id, cb, opt) => get(`/api/tasks/${id}`, null, cb, opt)
}
