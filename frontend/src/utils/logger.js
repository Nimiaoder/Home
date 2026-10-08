// 前端日誌：依等級輸出到瀏覽器 Console，方便追蹤與除錯。
// 開發模式預設顯示 debug 以上；正式環境預設只顯示 info 以上。
// 需要時在 Console 執行 localStorage.setItem('logLevel', 'debug') 後重新整理，即可臨時開啟 debug。
const LEVELS = { debug: 0, info: 1, warn: 2, error: 3 }

function threshold() {
  try {
    const saved = localStorage.getItem('logLevel')
    if (saved && saved in LEVELS) return LEVELS[saved]
  } catch {
    // 讀不到 localStorage 就用預設值
  }
  return import.meta.env.DEV ? LEVELS.debug : LEVELS.info
}

function emit(level, scope, args) {
  if (LEVELS[level] < threshold()) return
  const time = new Date().toTimeString().slice(0, 8)
  const fn = level === 'warn' ? console.warn : level === 'error' ? console.error : console.log
  fn(`[${time}] [${level.toUpperCase()}] [${scope}]`, ...args)
}

/** 取得某個模組專用的 logger：const log = logger('McModsTab') */
export function logger(scope) {
  return {
    debug: (...args) => emit('debug', scope, args),
    info: (...args) => emit('info', scope, args),
    warn: (...args) => emit('warn', scope, args),
    error: (...args) => emit('error', scope, args)
  }
}
