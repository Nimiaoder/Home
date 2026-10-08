<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import VersionPicker from './VersionPicker.vue'
import { mcResourceApi } from '@/api/minecraft'
import { taskScope } from '@/api/tasks'
import { useTaskRunner } from '@/composables/useTask'
import { logger } from '@/utils/logger'
import { confirmDialog } from '@/utils/confirm'
import { formatBytes, formatDate } from '@/utils/format'
import { toast } from '@/utils/toast'

// 版本庫：下載各種核心到本機共用；建立伺服器時若已下載就不用再抓
const entries = ref([])
const pick = reactive({ type: 'PAPER', mcVersion: '', build: '' })
const log = logger('LibraryVersions')
const { state, track, resume } = useTaskRunner()

const pickModel = computed({
  get: () => pick,
  set: (v) => Object.assign(pick, v)
})

const loadLibrary = () => mcResourceApi.library((list) => (entries.value = list))
onMounted(() => {
  log.debug('*****LibraryVersions*****')
  loadLibrary()
  // 重新整理頁面後，接回後端還在進行的下載
  resume(taskScope.library, { onDone: () => toast.success('已加入版本庫') })
})

const already = computed(() =>
  entries.value.some((e) => e.type === pick.type && e.mcVersion === pick.mcVersion && (!pick.build || e.build === pick.build))
)
watch(() => state.active, (a) => { if (!a) loadLibrary() })

function download() {
  if (!pick.mcVersion) return toast.error('請先選擇版本')
  log.info('下載到版本庫', pick.type, pick.mcVersion, pick.build)
  mcResourceApi.libraryDownload({ type: pick.type, mcVersion: pick.mcVersion, build: pick.build }, (d) =>
    track(d.taskId, { title: `下載 ${pick.type} ${pick.mcVersion}`, onDone: () => toast.success('已加入版本庫') })
  )
}

async function remove(e) {
  const { ok } = await confirmDialog({
    title: '從版本庫刪除',
    message: `刪除 ${e.type} ${e.mcVersion}（${e.build}）？\n已建立的伺服器不受影響，之後需要時會重新下載。`,
    confirmText: '刪除', danger: true
  })
  if (!ok) return
  log.info('從版本庫刪除', e.type, e.mcVersion, e.build)
  mcResourceApi.libraryDelete({ type: e.type, mcVersion: e.mcVersion, build: e.build }, loadLibrary, { showSuccess: true })
}
</script>

<template>
  <div class="stack">
    <section class="card card-pad stack">
      <h3 class="section-title">下載新版本</h3>
      <VersionPicker v-model="pickModel" />
      <div class="row">
        <button class="btn sm" :disabled="state.active || !pick.mcVersion" @click="download">
          <AppIcon name="download" :size="16" /> {{ already ? '重新確認 / 補下載' : '下載到版本庫' }}
        </button>
        <span v-if="already" class="tag ok"><AppIcon name="check" :size="12" /> 已在版本庫</span>
      </div>
      <ProgressBar v-if="state.active" :percent="state.percent" :done="state.bytesDone" :total="state.bytesTotal" :speed="state.speed" :label="state.message || state.title" />
    </section>

    <section class="card">
      <div class="head"><h3 class="section-title" style="margin:0">已下載（{{ entries.length }}）</h3></div>
      <EmptyState v-if="!entries.length" icon="package" title="版本庫是空的" hint="下載的核心會放在 library/，建立伺服器時直接使用。" />
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>類型</th><th>Minecraft</th><th>建置</th><th>檔案</th><th>大小</th><th>下載時間</th><th /></tr></thead>
          <tbody>
            <tr v-for="e in entries" :key="e.type + e.mcVersion + e.build">
              <td><span class="tag info">{{ e.type }}</span></td>
              <td>{{ e.mcVersion }}</td>
              <td class="mono small">{{ e.build }}</td>
              <td class="small muted">{{ e.fileName }}</td>
              <td>{{ formatBytes(e.sizeBytes) }}</td>
              <td class="small muted">{{ formatDate(e.modifiedAt) }}</td>
              <td class="actions"><button class="btn ghost danger sm icon" title="刪除" @click="remove(e)"><AppIcon name="trash" :size="16" /></button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.head { padding: 16px 20px 4px; }
</style>
