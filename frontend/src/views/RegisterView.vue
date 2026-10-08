<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { toast } from '@/utils/toast'
import { logger } from '@/utils/logger'

const log = logger('RegisterView')
log.debug('*****RegisterView*****')

const router = useRouter()

const username = ref('')
const nickname = ref('')
const password = ref('')
const confirm = ref('')
const showPwd = ref(false)
const loading = ref(false)
const enabled = ref(true)   // 由系統參數決定，載入後更新

onMounted(() => {
  authApi.registerEnabled((v) => (enabled.value = !!v), { showError: false })
})

function submit() {
  const u = username.value.trim()
  if (!u || !password.value) return toast.error('請輸入帳號與密碼')
  if (!/^[A-Za-z0-9_.-]{3,50}$/.test(u)) return toast.error('帳號需為 3~50 個英數字或 _ . -')
  if (password.value.length < 6) return toast.error('密碼至少 6 個字元')
  if (password.value !== confirm.value) return toast.error('兩次輸入的密碼不一致')
  log.info('送出註冊', u)
  loading.value = true
  authApi.register(
    { username: u, password: password.value, nickname: nickname.value.trim() },
    () => router.replace({ name: 'login' }),
    { showSuccess: true, onFinally: () => (loading.value = false) }
  )
}
</script>

<template>
  <main class="login">
    <section class="brand">
      <div class="brand-mark">註冊</div>
      <div class="brand-copy">
        <h1>家用服務管理</h1>
        <p>建立帳號後，由管理員視需要開通更多權限。</p>
      </div>
    </section>

    <section class="panel">
      <form class="card" @submit.prevent="submit" novalidate>
        <header>
          <h2>註冊帳號</h2>
          <p>{{ enabled ? '填寫以下資料建立一般使用者帳號' : '目前未開放註冊帳號' }}</p>
        </header>

        <label class="field">
          <span>帳號</span>
          <input v-model="username" type="text" autocomplete="username" autofocus placeholder="3~50 個英數字" :disabled="!enabled" />
        </label>

        <label class="field">
          <span>暱稱（選填）</span>
          <input v-model="nickname" type="text" autocomplete="nickname" placeholder="未填則同帳號" :disabled="!enabled" />
        </label>

        <label class="field">
          <span>密碼</span>
          <div class="pwd">
            <input v-model="password" :type="showPwd ? 'text' : 'password'" autocomplete="new-password" placeholder="至少 6 個字元" :disabled="!enabled" />
            <button type="button" class="eye" @click="showPwd = !showPwd">{{ showPwd ? '隱藏' : '顯示' }}</button>
          </div>
        </label>

        <label class="field">
          <span>確認密碼</span>
          <input v-model="confirm" :type="showPwd ? 'text' : 'password'" autocomplete="new-password" placeholder="再輸入一次密碼" :disabled="!enabled" />
        </label>

        <button class="btn submit" :disabled="loading || !enabled">{{ loading ? '註冊中…' : '註冊' }}</button>

        <p class="switch">已經有帳號？<RouterLink :to="{ name: 'login' }">返回登入</RouterLink></p>
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
.switch { text-align: center; font-size: 14px; color: var(--muted); }
.switch a { color: var(--accent); font-weight: 600; }

@media (max-width: 820px) {
  .login { grid-template-columns: 1fr; grid-template-rows: auto 1fr; }
  .brand { padding: 24px; }
  .brand-copy { display: none; }
  .panel { padding: 24px 16px; align-items: start; }
}
</style>
