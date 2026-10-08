// server.properties 常用設定的表單定義（key 與 Minecraft 官方一致）。
// 要在表單多顯示一個設定：加一筆即可；沒列在這裡的設定會出現在「進階：全部屬性」。
export const COMMON_PROPERTIES = [
  { key: 'motd', label: '伺服器訊息 (MOTD)', type: 'text', def: 'A Minecraft Server' },
  { key: 'max-players', label: '人數上限', type: 'number', def: '20' },
  { key: 'gamemode', label: '預設遊戲模式', type: 'select', def: 'survival',
    options: [['survival', '生存'], ['creative', '創造'], ['adventure', '冒險'], ['spectator', '旁觀']] },
  { key: 'difficulty', label: '難度', type: 'select', def: 'easy',
    options: [['peaceful', '和平'], ['easy', '簡單'], ['normal', '普通'], ['hard', '困難']] },
  { key: 'pvp', label: '允許玩家互相攻擊 (PVP)', type: 'bool', def: 'true' },
  { key: 'online-mode', label: '正版驗證', type: 'bool', def: 'true', hint: '關閉後任何人都能用任意名稱登入，請謹慎。' },
  { key: 'white-list', label: '啟用白名單', type: 'bool', def: 'false' },
  { key: 'allow-flight', label: '允許飛行', type: 'bool', def: 'false' },
  { key: 'hardcore', label: '極限模式', type: 'bool', def: 'false' },
  { key: 'enable-command-block', label: '啟用指令方塊', type: 'bool', def: 'false' },
  { key: 'allow-nether', label: '啟用地獄', type: 'bool', def: 'true' },
  { key: 'view-distance', label: '視距（區塊）', type: 'number', def: '10' },
  { key: 'simulation-distance', label: '模擬距離（區塊）', type: 'number', def: '10' },
  { key: 'spawn-protection', label: '出生點保護半徑', type: 'number', def: '16' },
  { key: 'level-seed', label: '地圖種子（僅新地圖有效）', type: 'text', def: '' }
]

// 由系統管理、不讓使用者在這裡改的 key
export const MANAGED_KEYS = {
  'server-port': '請在「設定 → 一般」修改連接埠',
  'level-name': '請在「地圖」分頁選擇使用中的地圖'
}
