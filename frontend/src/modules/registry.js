// 首頁的功能分類。新增一個大類：在這裡加一筆，並在 router 註冊它的路由即可。
// roles：可看到的角色（省略 = 所有登入者）
export const categories = [
  {
    key: 'gameserver',
    title: '遊戲伺服器',
    desc: '建立並管理遊戲伺服器：版本、地圖、模組、主控台',
    icon: 'gamepad',
    to: { name: 'games' },
    roles: ['ADMIN']
  }
]
