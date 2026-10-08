import { get, post, upload } from './request'

// Minecraft 模組的所有後端 API 對應（只做 url 對應，其餘交給共用 request）
const B = '/api/minecraft'
const S = (id) => `${B}/servers/${id}`

/** 伺服器本身：CRUD、電源、主控台、設定 */
export const mcServerApi = {
  list: (cb, opt) => get(`${B}/servers`, null, cb, opt),
  get: (id, cb, opt) => get(S(id), null, cb, opt),
  create: (body, cb, opt) => post(`${B}/servers`, body, cb, opt),
  update: (id, body, cb, opt) => post(`${S(id)}/update`, body, cb, opt),
  remove: (id, deleteFiles, cb, opt) => post(`${S(id)}/delete`, { deleteFiles }, cb, opt),
  acceptEula: (id, cb, opt) => post(`${S(id)}/eula/accept`, null, cb, opt),
  history: (id, cb, opt) => get(`${S(id)}/history`, null, cb, opt),
  changeVersion: (id, body, cb, opt) => post(`${S(id)}/version/change`, body, cb, opt),
  retryInstall: (id, cb, opt) => post(`${S(id)}/version/retry`, null, cb, opt),
  power: (id, action, cb, opt) => post(`${S(id)}/power`, { action }, cb, opt),
  status: (id, cb, opt) => get(`${S(id)}/status`, null, cb, opt),
  command: (id, command, cb, opt) => post(`${S(id)}/console/command`, { command }, cb, opt),
  downloadLog: (id, cb, opt) => post(`${S(id)}/logs/download`, null, cb, opt),
  properties: (id, cb, opt) => get(`${S(id)}/properties`, null, cb, opt),
  saveProperties: (id, values, cb, opt) => post(`${S(id)}/properties`, { values }, cb, opt),
  consoleStreamUrl: (id, after, epoch) => `${S(id)}/console/stream?after=${after}&epoch=${epoch}`
}

/** 地圖與備份 */
export const mcWorldApi = {
  list: (id, cb, opt) => get(`${S(id)}/worlds`, null, cb, opt),
  upload: (id, file, name, cb, opt) =>
    upload(`${S(id)}/worlds/upload?filename=${encodeURIComponent(file.name)}${name ? `&name=${encodeURIComponent(name)}` : ''}`, file, cb, opt),
  activate: (id, name, cb, opt) => post(`${S(id)}/worlds/activate`, { name }, cb, opt),
  remove: (id, name, cb, opt) => post(`${S(id)}/worlds/delete`, { name }, cb, opt),
  exportTicket: (id, name, cb, opt) => post(`${S(id)}/worlds/export`, { name }, cb, opt),
  backup: (id, name, cb, opt) => post(`${S(id)}/worlds/backup`, { name }, cb, opt),
  backups: (id, cb, opt) => get(`${S(id)}/backups`, null, cb, opt),
  restore: (id, file, cb, opt) => post(`${S(id)}/backups/restore`, { file }, cb, opt),
  removeBackup: (id, file, cb, opt) => post(`${S(id)}/backups/delete`, { file }, cb, opt),
  backupTicket: (id, file, cb, opt) => post(`${S(id)}/backups/download`, { file }, cb, opt)
}

/** 模組 / 插件 */
export const mcModApi = {
  list: (id, cb, opt) => get(`${S(id)}/mods`, null, cb, opt),
  upload: (id, file, cb, opt) => upload(`${S(id)}/mods/upload?filename=${encodeURIComponent(file.name)}`, file, cb, opt),
  toggle: (id, file, enabled, cb, opt) => post(`${S(id)}/mods/toggle`, { file, enabled }, cb, opt),
  remove: (id, file, cb, opt) => post(`${S(id)}/mods/delete`, { file }, cb, opt),
  search: (id, query, offset, cb, opt) => get(`${S(id)}/mods/search`, { query, offset }, cb, opt),
  install: (id, projectId, cb, opt) => post(`${S(id)}/mods/install`, { projectId }, cb, opt)
}

/** 資源庫：版本查詢、版本庫、Java */
export const mcResourceApi = {
  types: (cb, opt) => get(`${B}/types`, null, cb, opt),
  versions: (type, cb, opt) => get(`${B}/versions`, { type }, cb, opt),
  builds: (type, mcVersion, cb, opt) => get(`${B}/versions/builds`, { type, mcVersion }, cb, opt),
  library: (cb, opt) => get(`${B}/library`, null, cb, opt),
  libraryDownload: (body, cb, opt) => post(`${B}/library/download`, body, cb, opt),
  libraryDelete: (body, cb, opt) => post(`${B}/library/delete`, body, cb, opt),
  javaList: (refresh, cb, opt) => get(`${B}/java`, { refresh }, cb, opt),
  javaInstall: (major, cb, opt) => post(`${B}/java/install`, { major }, cb, opt),
  javaRemove: (path, cb, opt) => post(`${B}/java/remove`, { path }, cb, opt)
}
