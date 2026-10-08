<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'
import { logger } from '@/utils/logger'

const log = logger('LoginView')
log.debug('*****LoginView*****')

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const showPwd = ref(false)
const loading = ref(false)

function submit() {
  if (!username.value.trim() || !password.value) {
    toast.error('請輸入帳號與密碼')
    return
  }
  log.info('送出登入', username.value.trim())
  loading.value = true
  auth.login(username.value.trim(), password.value, {
    onSuccess: () => router.replace(route.query.redirect || '/'),
    onFinally: () => (loading.value = false)
  })
}
</script>

<template>
  <main class="login">
    <section class="brand">
      <div class="brand-mark">登入</div>
      <div class="brand-copy">
        <h1>家用服務管理</h1>
        <p>登入後使用你有權限的功能。</p>
      </div>
    </section>

    <section class="panel">
      <form class="card" @submit.prevent="submit" novalidate>
        <header>
          <h2>登入</h2>
          <p>輸入帳號密碼以繼續</p>
        </header>

        <label class="field">
          <span>帳號</span>
          <input v-model="username" type="text" autocomplete="username" autofocus placeholder="輸入帳號" />
        </label>

        <label class="field">
          <span>密碼</span>
          <div class="pwd">
            <input v-model="password" :type="showPwd ? 'text' : 'password'" autocomplete="current-password" placeholder="輸入密碼" />
            <button type="button" class="eye" @click="showPwd = !showPwd">{{ showPwd ? '隱藏' : '顯示' }}</button>
          </div>
        </label>

        <button class="btn submit" :disabled="loading">{{ loading ? '登入中…' : '登入' }}</button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login { min-height: 100%; display: grid; grid-template-columns: minmax(360px, 5fr) 6fr; }

.brand {
  position: relative; overflow: hidden; background: var(--deep); color: #e7f2ef;
  padding: 48px; display: flex; flex-direction: column; justify-content: space-between;
}
.brand-mark { font-weight: 700; font-size: 18px; letter-spacing: .02em; position: relative; z-index: 1; }
.brand-copy { position: relative; z-index: 1; max-width: 360px; }
.brand-copy h1 { font-size: 34px; line-height: 1.3; font-weight: 700; margin-bottom: 14px; }
.brand-copy p { color: #9fbcb6; }

.panel { display: grid; place-items: center; padding: 32px; }
.card {
  width: min(100%, 400px); background: var(--surface); border: 1px solid var(--line);
  border-radius: 20px; padding: 36px 32px; display: flex; flex-direction: column; gap: 20px;
  box-shadow: 0 1px 3px rgba(15, 42, 46, .06);
}
header h2 { font-size: 24px; }
header p { color: var(--muted); font-size: 14px; margin-top: 4px; }

.field { display: flex; flex-direction: column; gap: 6px; font-size: 14px; font-weight: 500; }
.field input {
  height: 46px; padding: 0 14px; width: 100%; border: 1px solid var(--line); border-radius: var(--radius);
  background: var(--paper); transition: border-color .15s, background .15s;
}
.field input:focus { outline: none; border-color: var(--accent); background: #fff; box-shadow: 0 0 0 3px var(--accent-soft); }
.pwd { position: relative; }
.pwd input { padding-right: 64px; }
.eye {
  position: absolute; right: 6px; top: 6px; height: 34px; padding: 0 10px; border: 0; border-radius: 8px;
  background: transparent; color: var(--muted); font-size: 13px; cursor: pointer;
}
.eye:hover { color: var(--accent); }
.submit { margin-top: 4px; width: 100%; }

@media (max-width: 820px) {
  .login { grid-template-columns: 1fr; grid-template-rows: auto 1fr; }
  .brand { padding: 24px; }
  .brand-copy { display: none; }
  .panel { padding: 24px 16px; align-items: start; }
}
</style>
