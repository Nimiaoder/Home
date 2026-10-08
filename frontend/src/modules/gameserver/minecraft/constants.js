// Minecraft 模組共用的常數與顯示對應

/** 執行狀態 → 顯示文字與色調（tone 對應 .tag 的 ok / warn / err / info） */
export const STATE_INFO = {
  STOPPED: { label: '已停止', tone: '', dot: '#9aa8a6' },
  STARTING: { label: '啟動中', tone: 'warn', dot: '#d9a21b' },
  RUNNING: { label: '運行中', tone: 'ok', dot: '#12a37f' },
  STOPPING: { label: '停止中', tone: 'warn', dot: '#d9a21b' },
  CRASHED: { label: '異常結束', tone: 'err', dot: '#c8452f' }
}

export const INSTALL_INFO = {
  INSTALLING: { label: '安裝中', tone: 'info', dot: '#2563a8' },
  FAILED: { label: '安裝失敗', tone: 'err', dot: '#c8452f' }
}

/** 取得一個伺服器要顯示的狀態（安裝狀態優先於執行狀態） */
export function statusOf(server) {
  if (!server) return STATE_INFO.STOPPED
  if (server.installState !== 'READY') return INSTALL_INFO[server.installState] || STATE_INFO.STOPPED
  return STATE_INFO[server.runtime?.state] || STATE_INFO.STOPPED
}

/** 是否正在執行（含啟動中 / 停止中） */
export function isAlive(server) {
  const s = server?.runtime?.state
  return s === 'STARTING' || s === 'RUNNING' || s === 'STOPPING'
}

/** 主控台快捷指令 */
export const QUICK_COMMANDS = ['list', 'save-all', 'time set day', 'weather clear']

/** 建議的 JVM 參數（G1GC，Paper 社群常用設定的精簡版） */
export const G1_FLAGS =
  '-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+DisableExplicitGC ' +
  '-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 ' +
  '-XX:InitiatingHeapOccupancyPercent=15 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1'
