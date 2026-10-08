// Minecraft 管理的路由。受保護：僅管理員（後端也有同樣限制）
const meta = { roles: ['ADMIN'] }

export default [
  { path: 'games/minecraft', name: 'mc-servers', component: () => import('./views/McServersView.vue'), meta },
  { path: 'games/minecraft/library', name: 'mc-library', component: () => import('./views/McLibraryView.vue'), meta },
  {
    path: 'games/minecraft/servers/:id',
    component: () => import('./views/McServerLayout.vue'),
    meta,
    children: [
      { path: '', name: 'mc-server', redirect: (to) => ({ name: 'mc-overview', params: to.params }) },
      { path: 'overview', name: 'mc-overview', component: () => import('./views/tabs/McOverviewTab.vue') },
      { path: 'console', name: 'mc-console', component: () => import('./views/tabs/McConsoleTab.vue') },
      { path: 'worlds', name: 'mc-worlds', component: () => import('./views/tabs/McWorldsTab.vue') },
      { path: 'mods', name: 'mc-mods', component: () => import('./views/tabs/McModsTab.vue') },
      { path: 'version', name: 'mc-version', component: () => import('./views/tabs/McVersionTab.vue') },
      { path: 'settings', name: 'mc-settings', component: () => import('./views/tabs/McSettingsTab.vue') }
    ]
  }
]
