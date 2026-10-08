import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gameServerRoutes from '@/modules/gameserver/routes'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  {
    // 登入後的頁面都放在 AppShell（共用頂部列）底下
    path: '/',
    component: () => import('@/layouts/AppShell.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
      // 新增功能分類：把它的 routes 展開在這裡（meta.roles 可限制角色）
      ...gameServerRoutes
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && auth.isLoggedIn) return { name: 'home' }
  // 角色限制：任何一層 route 設了 meta.roles 且目前角色不在其中，就回首頁
  const role = auth.user?.role
  if (to.matched.some((r) => r.meta.roles && !r.meta.roles.includes(role))) return { name: 'home' }
})

export default router
