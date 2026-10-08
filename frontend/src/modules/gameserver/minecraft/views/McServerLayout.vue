<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/ui/AppIcon.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import TabNav from '@/components/ui/TabNav.vue'
import McStatusBadge from '../components/McStatusBadge.vue'
import { mcServerApi } from '@/api/minecraft'
import { usePolling } from '@/composables/usePolling'
import { useTaskRunner } from '@/composables/useTask'
import { usePower } from '../composables/usePower'
import { provideServer } from '../composables/useServerContext'
import { isAlive } from '../constants'
import { toast } from '@/utils/toast'
import { logger } from '@/utils/logger'

const log = logger('McServerLayout')
log.debug('*****McServerLayout*****')

// 單一伺服器的外框：標題 + 電源按鈕 + 分頁。資料每 3 秒更新一次，並透過 provide 分享給各分頁。
const route = useRoute()
const router = useRouter()
const id = computed(() => Number(route.params.id))
const server = ref(null)
let loadedOnce = false

function reload() {
  return mcServerApi.get(id.value, (d) => {
    server.value = d
    loadedOnce = true
  }, {
    showError: !loadedOnce,
    onError: () => { if (!loadedOnce) router.replace({ name: 'mc-servers' }) }
  })
}
usePolling(reload, 3000)
watch(id, () => { server.value = null; loadedOnce = false; reload() })
provideServer({ server, reload, id })

// 安裝進度
const { state: install, track } = useTaskRunner()
let trackedTask = ''
watch(() => server.value?.installTaskId, (tid) => {
  if (!tid || tid === trackedTask) return
  trackedTask = tid
  track(tid, { title: '安裝中', onDone: () => { toast.success('安裝完成'); reload() }, onFail: reload })
}, { immediate: true })

const { busy, act } = usePower(() => server.value, reload)
const alive = computed(() => isAlive(server.value))
const ready = computed(() => server.value?.installState === 'READY')

const contentLabel = computed(() => (server.value?.contentKind === 'PLUGIN' ? '插件' : '模組'))
const tabs = computed(() => {
  const p = { id: id.value }
  return [
    { to: { name: 'mc-overview', params: p }, label: '總覽', icon: 'server' },
    { to: { name: 'mc-console', params: p }, label: '主控台', icon: 'terminal' },
    { to: { name: 'mc-worlds', params: p }, label: '地圖', icon: 'globe' },
    { to: { name: 'mc-mods', params: p }, label: contentLabel.value, icon: 'puzzle' },
    { to: { name: 'mc-version', params: p }, label: '版本', icon: 'tag' },
    { to: { name: 'mc-settings', params: p }, label: '設定', icon: 'sliders' }
  ]
})

function retry() {
  mcServerApi.retryInstall(id.value, () => reload())
}
</script>

<template>
  <main class="page">
    <template v-if="server">
      <header class="head">
        <RouterLink :to="{ name: 'mc-servers' }" class="back"><AppIcon name="back" :size="16" /> Minecraft 伺服器</RouterLink>
        <div class="row">
          <h1>{{ server.name }}</h1>
          <McStatusBadge :server="server" />
          <span class="tag">{{ server.typeName }} {{ server.mcVersion }}</span>
          <div class="spacer" />
          <div class="row tight">
            <button v-if="!alive" class="btn sm" :disabled="busy || !ready" @click="act('start')"><AppIcon name="play" :size="14" /> 啟動</button>
            <template v-else>
              <button class="btn sm ghost" :disabled="busy || server.runtime.state === 'STOPPING'" @click="act('stop')"><AppIcon name="stop" :size="14" /> 停止</button>
              <button class="btn sm ghost" :disabled="busy || server.runtime.state === 'STOPPING'" @click="act('restart')"><AppIcon name="restart" :size="14" /> 重啟</button>
              <button class="btn sm ghost danger" :disabled="busy" @click="act('kill')"><AppIcon name="power" :size="14" /> 強制結束</button>
            </template>
          </div>
        </div>
      </header>

      <div v-if="server.installState === 'INSTALLING'" class="notice stack banner">
        <strong>正在下載並安裝伺服器檔案…</strong>
        <ProgressBar :percent="install.percent" :done="install.bytesDone" :total="install.bytesTotal" :speed="install.speed" :label="install.message" />
      </div>
      <div v-else-if="server.installState === 'FAILED'" class="notice err banner">
        <div><strong>安裝失敗</strong>：{{ server.installError }}</div>
        <div class="row tight" style="margin-top:10px">
          <button class="btn sm" @click="retry">重新安裝</button>
          <RouterLink :to="{ name: 'mc-version', params: { id } }" class="btn sm ghost">改選其他版本</RouterLink>
        </div>
      </div>

      <TabNav :tabs="tabs" />
      <RouterView />
    </template>
    <p v-else class="muted">載入中…</p>
  </main>
</template>

<style scoped>
.head { margin-bottom: 20px; }
.back { display: inline-flex; align-items: center; gap: 4px; color: var(--muted); font-size: 14px; margin-bottom: 10px; }
.back:hover { color: var(--accent); }
h1 { font-size: 26px; }
.banner { margin-bottom: 20px; }
</style>
