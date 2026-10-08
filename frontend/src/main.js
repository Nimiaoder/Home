import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { setUnauthorizedHandler } from './api/request'
import { logger } from './utils/logger'
import './styles/main.css'

const log = logger('main')

const app = createApp(App)
app.use(createPinia())
app.use(router)

// token 失效（後端回 401）→ 清除登入狀態並回登入頁
setUnauthorizedHandler(() => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn) return
  log.warn('登入已失效（401），回到登入頁')
  auth.logout()
  router.replace({ name: 'login' })
})

app.mount('#app')
log.info('前端已啟動', import.meta.env.MODE)
