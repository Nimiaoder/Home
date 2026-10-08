// 「遊戲伺服器」底下可選的遊戲。新增遊戲：加一筆並在 routes 註冊它的路由。
export const games = [
  {
    id: 'minecraft',
    name: 'Minecraft',
    desc: 'Java 版伺服器：多個伺服器、版本管理、地圖、模組與即時主控台',
    icon: 'cube',
    to: { name: 'mc-servers' }
  }
]
