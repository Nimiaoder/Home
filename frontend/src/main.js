import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { setUnauthorizedHandler } from './api/request'
import './styles/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// token 失效（後端回 401）→ 清除登入狀態並回登入頁
setUnauthorizedHandler(() => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn) return
  auth.logout()
  router.replace({ name: 'login' })
})

app.mount('#app')
