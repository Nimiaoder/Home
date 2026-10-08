<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { categories } from '@/modules/registry'
import AppIcon from '@/components/ui/AppIcon.vue'
import { logger } from '@/utils/logger'

const log = logger('HomeView')
log.debug('*****HomeView*****')

const auth = useAuthStore()

// 只顯示目前角色看得到的分類
const visible = computed(() => categories.filter((c) => !c.roles || c.roles.includes(auth.user?.role)))
</script>

<template>
  <main class="page narrow">
    <h1 class="hello">你好，{{ auth.user?.nickname }}</h1>
    <p class="muted sub">帳號 {{ auth.user?.username }} · 權限 {{ auth.user?.role }}</p>

    <h2 class="section-title">功能</h2>
    <div v-if="visible.length" class="cats">
      <RouterLink v-for="c in visible" :key="c.key" :to="c.to" class="cat card">
        <span class="ico"><AppIcon :name="c.icon" :size="26" /></span>
        <strong>{{ c.title }}</strong>
        <span class="muted small">{{ c.desc }}</span>
      </RouterLink>
    </div>
    <p v-else class="muted">目前沒有可使用的功能。</p>
  </main>
</template>

<style scoped>
.hello { font-size: 28px; }
.sub { margin: 4px 0 32px; }
.cats { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.cat {
  display: flex; flex-direction: column; gap: 6px; padding: 22px; transition: border-color .15s;
}
.cat:hover { border-color: var(--accent); }
.ico { width: 48px; height: 48px; border-radius: 14px; display: grid; place-items: center; background: var(--accent-soft); color: var(--accent); margin-bottom: 8px; }
.cat strong { font-size: 17px; }
</style>
