// Minecraft 管理的路由。受保護：僅管理員（後端也有同樣限制）
const meta = { roles: ['ADMIN'] }

export default [
  { path: 'games/minecraft', name: 'mc-servers', component: () => import('./views/McServersView.vue'), meta: { ...meta, title: 'Minecraft 伺服器' } },
  { path: 'games/minecraft/library', name: 'mc-library', component: () => import('./views/McLibraryView.vue'), meta: { ...meta, title: '資源庫' } },
  {
    path: 'games/minecraft/servers/:id',
    component: () => import('./views/McServerLayout.vue'),
    meta,
    children: [
      { path: '', name: 'mc-server', redirect: (to) => ({ name: 'mc-overview', params: to.params }) },
      { path: 'overview', name: 'mc-overview', component: () => import('./views/tabs/McOverviewTab.vue'), meta: { title: '伺服器總覽' } },
      { path: 'console', name: 'mc-console', component: () => import('./views/tabs/McConsoleTab.vue'), meta: { title: '主控台' } },
      { path: 'worlds', name: 'mc-worlds', component: () => import('./views/tabs/McWorldsTab.vue'), meta: { title: '地圖管理' } },
      { path: 'mods', name: 'mc-mods', component: () => import('./views/tabs/McModsTab.vue'), meta: { title: '模組與插件' } },
      { path: 'version', name: 'mc-version', component: () => import('./views/tabs/McVersionTab.vue'), meta: { title: '版本管理' } },
      { path: 'settings', name: 'mc-settings', component: () => import('./views/tabs/McSettingsTab.vue'), meta: { title: '伺服器設定' } }
    ]
  }
]
