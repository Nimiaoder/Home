<script setup>
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import UploadButton from '@/components/ui/UploadButton.vue'
import { useServer } from '../../composables/useServerContext'
import { useTaskRunner } from '@/composables/useTask'
import { mcWorldApi } from '@/api/minecraft'
import { confirmDialog } from '@/utils/confirm'
import { downloadByTicket } from '@/utils/download'
import { formatBytes, formatDate } from '@/utils/format'
import { isAlive } from '../../constants'
import { toast } from '@/utils/toast'

const { server, id } = useServer()
const alive = computed(() => isAlive(server.value))

const worlds = ref([])
const backups = ref([])
const { state: task, track } = useTaskRunner()

function load() {
  mcWorldApi.list(id.value, (d) => (worlds.value = d))
  mcWorldApi.backups(id.value, (d) => (backups.value = d))
}
onMounted(load)

// ---- 匯入地圖 ----
const pendingFile = ref(null)
const importName = ref('')
const uploading = ref(false)
const uploadPct = ref(0)

function pick(file) {
  if (!file.name.toLowerCase().endsWith('.zip')) return toast.error('請選擇 .zip 格式的地圖壓縮檔')
  pendingFile.value = file
  importName.value = ''
}
function doImport() {
  const file = pendingFile.value
  pendingFile.value = null
  uploading.value = true
  uploadPct.value = 0
  mcWorldApi.upload(id.value, file, importName.value.trim(), (d) => {
    track(d.taskId, { title: `匯入 ${file.name}`, onDone: () => { toast.success('地圖已匯入'); load() } })
  }, { onProgress: (p) => (uploadPct.value = p), onFinally: () => (uploading.value = false) })
}

// ---- 地圖操作 ----
const activate = (w) => mcWorldApi.activate(id.value, w.name, load, { showSuccess: true })

function backup(w) {
  mcWorldApi.backup(id.value, w.name, (d) =>
    track(d.taskId, { title: `備份 ${w.name}`, onDone: () => { toast.success('備份完成'); load() } }))
}

async function exportWorld(w) {
  if (alive.value) {
    const { ok } = await confirmDialog({
      title: '伺服器執行中',
      message: '直接下載執行中的地圖，內容可能不一致。\n建議改用「備份」，它會先通知伺服器存檔。仍要直接下載嗎？',
      confirmText: '仍要下載'
    })
    if (!ok) return
  }
  mcWorldApi.exportTicket(id.value, w.name, downloadByTicket)
}

async function removeWorld(w) {
  const { ok } = await confirmDialog({
    title: `刪除地圖「${w.name}」`,
    message: (w.active ? '這是目前使用中的地圖，刪除後下次啟動會產生全新的地圖。\n' : '') + '此動作無法復原，建議先備份。',
    confirmText: '刪除', danger: true
  })
  if (ok) mcWorldApi.remove(id.value, w.name, load, { showSuccess: true })
}

// ---- 備份操作 ----
const TAGS = { 'before-restore': '還原前自動備份', 'before-version-change': '切換版本前自動備份' }

async function restore(b) {
  const { ok } = await confirmDialog({
    title: `還原「${b.world}」`,
    message: `用 ${formatDate(b.createdAt)} 的備份覆蓋目前的「${b.world}」。\n系統會先自動備份目前的內容，所以可以反悔。`,
    confirmText: '還原'
  })
  if (!ok) return
  mcWorldApi.restore(id.value, b.fileName, (d) =>
    track(d.taskId, { title: `還原 ${b.world}`, onDone: () => { toast.success('已還原'); load() } }))
}
const downloadBackup = (b) => mcWorldApi.backupTicket(id.value, b.fileName, downloadByTicket)

async function removeBackup(b) {
  const { ok } = await confirmDialog({ title: '刪除備份', message: b.fileName, confirmText: '刪除', danger: true })
  if (ok) mcWorldApi.removeBackup(id.value, b.fileName, load, { showSuccess: true })
}
</script>

<template>
  <div class="stack">
    <section class="card">
      <div class="head row">
        <h3 class="section-title" style="margin:0">地圖</h3>
        <div class="spacer" />
        <UploadButton accept=".zip" label="匯入地圖 (.zip)" :disabled="uploading || task.active" @pick="pick" />
      </div>
      <div v-if="uploading || task.active" class="prog">
        <ProgressBar v-if="uploading" :percent="uploadPct" :label="`上傳中 ${uploadPct}%`" />
        <ProgressBar v-else :percent="task.percent" :label="task.message || task.title" />
      </div>

      <EmptyState v-if="!worlds.length" icon="globe" title="還沒有地圖"
                  hint="伺服器第一次啟動後會自動產生地圖；你也可以匯入既有的地圖壓縮檔（需包含 level.dat）。" />
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>名稱</th><th>大小</th><th>最後修改</th><th /></tr></thead>
          <tbody>
            <tr v-for="w in worlds" :key="w.name">
              <td>
                <strong>{{ w.name }}</strong>
                <span v-if="w.active" class="tag ok" style="margin-left:8px">使用中</span>
                <span v-if="w.hasNether" class="tag" style="margin-left:6px">下界</span>
                <span v-if="w.hasEnd" class="tag" style="margin-left:6px">終界</span>
              </td>
              <td>{{ formatBytes(w.sizeBytes) }}</td>
              <td class="muted small">{{ formatDate(w.modifiedAt) }}</td>
              <td class="actions">
                <div class="row tight" style="justify-content:flex-end">
                  <button v-if="!w.active" class="btn sm ghost" @click="activate(w)">設為使用中</button>
                  <button class="btn sm ghost" :disabled="task.active" @click="backup(w)"><AppIcon name="archive" :size="14" /> 備份</button>
                  <button class="btn sm ghost icon" title="下載" @click="exportWorld(w)"><AppIcon name="download" :size="15" /></button>
                  <button class="btn sm ghost danger icon" title="刪除" :disabled="alive" @click="removeWorld(w)"><AppIcon name="trash" :size="15" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="alive && worlds.length" class="foot muted small">伺服器執行中：無法刪除地圖；「設為使用中」需重新啟動後生效。</p>
    </section>

    <section class="card">
      <div class="head"><h3 class="section-title" style="margin:0">備份（{{ backups.length }}）</h3></div>
      <EmptyState v-if="!backups.length" icon="archive" title="還沒有備份" hint="在上方地圖按「備份」即可，執行中的伺服器也能備份。" />
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>地圖</th><th>備份時間</th><th>大小</th><th /></tr></thead>
          <tbody>
            <tr v-for="b in backups" :key="b.fileName">
              <td>
                <strong>{{ b.world }}</strong>
                <span v-if="b.tag" class="tag info" style="margin-left:8px">{{ TAGS[b.tag] || b.tag }}</span>
              </td>
              <td class="muted small">{{ formatDate(b.createdAt) }}</td>
              <td>{{ formatBytes(b.sizeBytes) }}</td>
              <td class="actions">
                <div class="row tight" style="justify-content:flex-end">
                  <button class="btn sm ghost" :disabled="alive || task.active" :title="alive ? '請先停止伺服器' : ''" @click="restore(b)">還原</button>
                  <button class="btn sm ghost icon" title="下載" @click="downloadBackup(b)"><AppIcon name="download" :size="15" /></button>
                  <button class="btn sm ghost danger icon" title="刪除" @click="removeBackup(b)"><AppIcon name="trash" :size="15" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <BaseModal :model-value="!!pendingFile" title="匯入地圖" width="460px" @update:model-value="pendingFile = null">
      <div class="stack">
        <p class="small muted">檔案：{{ pendingFile?.name }}（{{ formatBytes(pendingFile?.size) }}）</p>
        <div class="form-field">
          <label>地圖名稱（選填）</label>
          <input v-model="importName" class="input" placeholder="留空則沿用壓縮檔內的資料夾名稱" @keyup.enter="doImport" />
          <span class="hint">匯入後可在列表按「設為使用中」，重新啟動就會載入這張地圖。</span>
        </div>
      </div>
      <template #footer>
        <button class="btn ghost sm" @click="pendingFile = null">取消</button>
        <button class="btn sm" @click="doImport">開始匯入</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.head { padding: 16px 20px 8px; }
.prog { padding: 4px 20px 14px; }
.foot { padding: 0 20px 14px; }
</style>
