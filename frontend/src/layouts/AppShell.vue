<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppIcon from '@/components/ui/AppIcon.vue'

// 登入後所有頁面共用的外框：頂部列 + 內容區
const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="shell">
    <header class="bar">
      <RouterLink :to="{ name: 'home' }" class="brand">Dev Console</RouterLink>
      <div class="who">
        <span class="avatar">{{ auth.user?.nickname?.slice(0, 1) }}</span>
        <span class="name">{{ auth.user?.nickname }}</span>
        <button class="btn ghost sm" @click="logout"><AppIcon name="logout" :size="16" /> 登出</button>
      </div>
    </header>
    <RouterView />
  </div>
</template>

<style scoped>
.bar {
  position: sticky; top: 0; z-index: 10; height: 60px; padding: 0 24px;
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255, 255, 255, .88); backdrop-filter: blur(8px); border-bottom: 1px solid var(--line);
}
.brand { font-weight: 800; letter-spacing: .2px; }
.who { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.avatar {
  width: 32px; height: 32px; border-radius: 50%; background: var(--accent-soft); color: var(--accent);
  display: grid; place-items: center; font-weight: 700;
}
@media (max-width: 560px) { .name { display: none; } }
</style>
