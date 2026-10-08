<script setup>
import { onMounted, ref, watch } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { mcResourceApi } from '@/api/minecraft'
import { taskScope } from '@/api/tasks'
import { useTaskRunner } from '@/composables/useTask'
import { logger } from '@/utils/logger'
import { confirmDialog } from '@/utils/confirm'
import { toast } from '@/utils/toast'

// Java 環境：只列出 java/ 資料夾內的 Java，並可下載 Temurin（Minecraft 26.1 起需要 Java 25）
const list = ref([])
const loading = ref(false)
const log = logger('LibraryJava')
const { state, track, resume } = useTaskRunner()

// major → 適用的 Minecraft 版本說明
const OPTIONS = [
  { major: 25, hint: 'Minecraft 26.1 以上' },
  { major: 21, hint: 'Minecraft 1.20.5 ～ 1.21.x' },
  { major: 17, hint: 'Minecraft 1.18 ～ 1.20.4' },
  { major: 8, hint: 'Minecraft 1.16.5 以下' }
]

function load(refresh = false) {
  loading.value = true
  mcResourceApi.javaList(refresh, (d) => (list.value = d), { onFinally: () => (loading.value = false) })
}
onMounted(() => {
  log.debug('*****LibraryJava*****')
  load(false)
  // 重新整理頁面後，接回後端還在進行的安裝
  resume(taskScope.java, { onDone: () => toast.success('Java 安裝完成') })
})
watch(() => state.active, (a) => { if (!a) load(true) })

const has = (major) => list.value.some((j) => j.major === major)

function install(major) {
  log.info('安裝 Java', major)
  mcResourceApi.javaInstall(major, (d) =>
    track(d.taskId, { title: `安裝 Java ${major}`, onDone: () => toast.success(`Java ${major} 安裝完成`) })
  )
}

async function remove(j) {
  const { ok } = await confirmDialog({ title: '移除 Java', message: `移除 ${j.path}？`, confirmText: '移除', danger: true })
  if (!ok) return
  log.info('移除 Java', j.path)
  mcResourceApi.javaRemove(j.path, () => load(true), { showSuccess: true })
}
</script>

<template>
  <div class="stack">
    <section class="card card-pad stack">
      <h3 class="section-title">下載安裝 Java（Eclipse Temurin）</h3>
      <p class="muted small">安裝到 java/ 資料夾，啟動伺服器時依版本自動選用。</p>
      <div class="opts">
        <div v-for="o in OPTIONS" :key="o.major" class="opt">
          <div>
            <strong>Java {{ o.major }}</strong>
            <div class="muted small">{{ o.hint }}</div>
          </div>
          <span v-if="has(o.major)" class="tag ok"><AppIcon name="check" :size="12" /> 已有</span>
          <button v-else class="btn sm soft" :disabled="state.active" @click="install(o.major)">
            <AppIcon name="download" :size="14" /> 安裝
          </button>
        </div>
      </div>
      <ProgressBar v-if="state.active" :percent="state.percent" :done="state.bytesDone" :total="state.bytesTotal" :speed="state.speed" :label="state.message || state.title" />
    </section>

    <section class="card">
      <div class="head row">
        <h3 class="section-title" style="margin:0">已安裝的 Java（{{ list.length }}）</h3>
        <div class="spacer" />
        <button class="btn ghost sm" :disabled="loading" @click="load(true)"><AppIcon name="restart" :size="14" /> 重新整理</button>
      </div>
      <EmptyState v-if="!list.length && !loading" icon="coffee" title="尚未安裝 Java" hint="從上方選擇版本下載。" />
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>版本</th><th>路徑</th><th /></tr></thead>
          <tbody>
            <tr v-for="j in list" :key="j.path">
              <td><strong>Java {{ j.major }}</strong> <span class="muted small">{{ j.version }}</span></td>
              <td class="mono small">{{ j.path }}</td>
              <td class="actions">
                <button v-if="j.managed" class="btn ghost danger sm icon" title="移除" @click="remove(j)"><AppIcon name="trash" :size="16" /></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.opts { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.opt { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 16px; border: 1px solid var(--line); border-radius: 12px; }
.head { padding: 16px 20px 4px; }
</style>
