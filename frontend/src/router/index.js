import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gameServerRoutes from '@/modules/gameserver/routes'
import { logger } from '@/utils/logger'

const log = logger('router')

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true, title: '登入' } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { public: true, title: '註冊' } },
  {
    // 登入後的頁面都放在 AppShell（共用頂部列）底下
    path: '/',
    component: () => import('@/layouts/AppShell.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '主頁' } },
      // 新增功能分類：把它的 routes 展開在這裡（meta.roles 可限制角色）
      ...gameServerRoutes
    ]
  },
  // 任何未定義路徑：未登入 → login；已登入 → 主頁
  { path: '/:pathMatch(.*)*', name: 'not-found', redirect: () => ({ name: useAuthStore().isLoggedIn ? 'home' : 'login' }) }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from) => {
  log.debug('*****導覽*****', from.fullPath, '→', to.fullPath)
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    log.info('尚未登入，導向登入頁', to.fullPath)
    // 只有已知頁面才帶 redirect，未知路徑直接到登入頁
    return to.matched.length && to.fullPath !== '/'
      ? { name: 'login', query: { redirect: to.fullPath } }
      : { name: 'login' }
  }
  // 已登入時進入登入／註冊頁 → 主頁
  if (to.meta.public && auth.isLoggedIn) return { name: 'home' }
  // 角色限制：任何一層 route 設了 meta.roles 且目前角色不在其中，就回首頁
  const role = auth.user?.role
  if (to.matched.some((r) => r.meta.roles && !r.meta.roles.includes(role))) {
    log.warn('角色', role, '沒有權限進入', to.fullPath)
    return { name: 'home' }
  }
})

// 瀏覽器分頁標題跟著目前頁面走（取最內層有設定 title 的路由）
router.afterEach((to) => {
  const title = [...to.matched].reverse().find((r) => r.meta.title)?.meta.title
  document.title = title ? `${title} · Home` : 'Home'
})

export default router
