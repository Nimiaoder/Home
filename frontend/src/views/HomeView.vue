<script setup>
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { demoApi } from '@/api/auth'
import { categories } from '@/modules/registry'
import AppIcon from '@/components/ui/AppIcon.vue'

const auth = useAuthStore()
const result = ref('')

// 只顯示目前角色看得到的分類
const visible = computed(() => categories.filter((c) => !c.roles || c.roles.includes(auth.user?.role)))

// 範例：呼叫受保護的 API。callBack 拿到的就是後端 ApiResponse.detail
const callHello = () => demoApi.hello((detail) => (result.value = JSON.stringify(detail, null, 2)))
const callAdmin = () => demoApi.admin((detail) => (result.value = JSON.stringify(detail, null, 2)))
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

    <details class="dev card">
      <summary>開發工具：測試受保護的 API</summary>
      <p class="muted small">點擊後會帶著登入憑證呼叫後端；管理員專用的 API 以一般帳號呼叫會收到「沒有權限」。</p>
      <div class="row">
        <button class="btn sm" @click="callHello">呼叫 /api/demo/hello</button>
        <button class="btn ghost sm" @click="callAdmin">呼叫 /api/demo/admin</button>
      </div>
      <pre v-if="result">{{ result }}</pre>
    </details>
  </main>
</template>

<style scoped>
.hello { font-size: 28px; }
.sub { margin: 4px 0 32px; }
.cats { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.cat {
  display: flex; flex-direction: column; gap: 6px; padding: 22px; transition: transform .15s, box-shadow .15s, border-color .15s;
}
.cat:hover { transform: translateY(-2px); border-color: var(--accent); box-shadow: 0 10px 28px rgba(14, 124, 102, .12); }
.ico { width: 48px; height: 48px; border-radius: 14px; display: grid; place-items: center; background: var(--accent-soft); color: var(--accent); margin-bottom: 8px; }
.cat strong { font-size: 17px; }
.dev { margin-top: 40px; padding: 16px 20px; }
.dev summary { cursor: pointer; font-weight: 600; font-size: 14px; }
.dev .row { margin-top: 12px; }
pre { margin: 14px 0 0; padding: 14px; background: var(--deep); color: #cfe8e1; border-radius: var(--radius); font-size: 13px; overflow-x: auto; }
</style>
