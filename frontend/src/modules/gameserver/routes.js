import minecraftRoutes from './minecraft/routes'

// 「遊戲伺服器」分類的所有路由（掛在 AppShell 底下）。新增遊戲：展開它的 routes 即可。
export default [
  {
    path: 'games',
    name: 'games',
    component: () => import('./GameServerView.vue'),
    meta: { roles: ['ADMIN'], title: '遊戲伺服器' }
  },
  ...minecraftRoutes
]
