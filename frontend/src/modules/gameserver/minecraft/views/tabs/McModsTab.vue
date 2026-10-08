<script setup>
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import SwitchToggle from '@/components/ui/SwitchToggle.vue'
import UploadButton from '@/components/ui/UploadButton.vue'
import { useServer } from '../../composables/useServerContext'
import { useTaskRunner } from '@/composables/useTask'
import { mcModApi } from '@/api/minecraft'
import { taskScope } from '@/api/tasks'
import { logger } from '@/utils/logger'
import { confirmDialog } from '@/utils/confirm'
import { formatBytes, formatNumber } from '@/utils/format'
import { toast } from '@/utils/toast'

const { server, id } = useServer()
const kindLabel = computed(() => (server.value.contentKind === 'PLUGIN' ? '插件' : '模組'))
const supported = computed(() => server.value.contentKind !== 'NONE')

const tab = ref('installed')
const info = ref({ files: [], contentDir: '' })
const uploading = ref(false)
const uploadPct = ref(0)
const log = logger('McModsTab')
const { state: task, track, resume } = useTaskRunner()

const load = () => supported.value && mcModApi.list(id.value, (d) => (info.value = d))
onMounted(() => {
  log.debug('*****McModsTab*****')
  load()
  // 重新整理頁面後，接回後端還在進行的安裝
  resume(taskScope.mods(id.value), { onDone: (t) => { toast.success(t.message || '安裝完成'); load() } })
})

// ---- 已安裝 ----
function upload(file) {
  if (!file.name.toLowerCase().endsWith('.jar')) return toast.error('只能上傳 .jar 檔案')
  log.info('上傳', file.name, formatBytes(file.size))
  uploading.value = true
  uploadPct.value = 0
  mcModApi.upload(id.value, file, load, {
    showSuccess: true, onProgress: (p) => (uploadPct.value = p), onFinally: () => (uploading.value = false)
  })
}
const toggle = (m, enabled) => mcModApi.toggle(id.value, m.fileName, enabled, load, { showSuccess: true })

async function remove(m) {
  const { ok } = await confirmDialog({ title: `刪除${kindLabel.value}`, message: m.fileName, confirmText: '刪除', danger: true })
  if (ok) mcModApi.remove(id.value, m.fileName, load, { showSuccess: true })
}

// ---- Modrinth 搜尋 ----
const query = ref('')
const hits = ref([])
const total = ref(0)
const searching = ref(false)
const searched = ref(false)

function search(more = false) {
  searching.value = true
  const offset = more ? hits.value.length : 0
  mcModApi.search(id.value, query.value.trim(), offset, (d) => {
    hits.value = more ? hits.value.concat(d.hits) : d.hits
    total.value = d.total
    searched.value = true
  }, { onFinally: () => (searching.value = false) })
}
function switchToSearch() {
  tab.value = 'search'
  if (!searched.value) search()
}

function install(h) {
  log.info('從 Modrinth 安裝', h.projectId, h.title)
  mcModApi.install(id.value, h.projectId, (d) =>
    track(d.taskId, { title: `安裝 ${h.title}`, onDone: (t) => { toast.success(t.message || '安裝完成'); load() } }))
}
</script>

<template>
  <div v-if="!supported" class="card">
    <EmptyState icon="puzzle" title="Vanilla 不支援模組或插件"
                hint="要用模組請改用 Fabric、Forge 或 NeoForge，要用插件請改用 Paper。可在「版本」分頁切換，地圖會保留。" />
  </div>

  <div v-else class="stack">
    <nav class="seg">
      <button :class="{ on: tab === 'installed' }" @click="tab = 'installed'">已安裝（{{ info.files.length }}）</button>
      <button :class="{ on: tab === 'search' }" @click="switchToSearch">從 Modrinth 安裝</button>
    </nav>

    <div v-if="task.active" class="card card-pad"><ProgressBar :percent="task.percent" :done="task.bytesDone" :total="task.bytesTotal" :speed="task.speed" :label="task.message || task.title" /></div>

    <!-- 已安裝 -->
    <section v-if="tab === 'installed'" class="card">
      <div class="head row">
        <span class="muted small">資料夾：{{ info.contentDir }}/　停用的檔案會加上 .disabled</span>
        <div class="spacer" />
        <UploadButton accept=".jar" :label="`上傳${kindLabel} (.jar)`" :disabled="uploading" @pick="upload" />
      </div>
      <div v-if="uploading" class="prog"><ProgressBar :percent="uploadPct" :label="`上傳中 ${uploadPct}%`" /></div>
      <EmptyState v-if="!info.files.length" icon="puzzle" :title="`還沒有安裝任何${kindLabel}`" :hint="`上傳 .jar，或到「從 Modrinth 安裝」搜尋。變更後需重新啟動伺服器。`" />
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>啟用</th><th>名稱</th><th>版本</th><th>大小</th><th /></tr></thead>
          <tbody>
            <tr v-for="m in info.files" :key="m.fileName" :class="{ off: !m.enabled }">
              <td><SwitchToggle :model-value="m.enabled" @update:model-value="toggle(m, $event)" /></td>
              <td>
                <strong>{{ m.name || m.fileName }}</strong>
                <div class="muted small mono">{{ m.fileName }}</div>
              </td>
              <td class="small">{{ m.version || '-' }}</td>
              <td>{{ formatBytes(m.sizeBytes) }}</td>
              <td class="actions"><button class="btn sm ghost danger icon" title="刪除" @click="remove(m)"><AppIcon name="trash" :size="15" /></button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Modrinth -->
    <section v-else class="stack">
      <form class="row" @submit.prevent="search(false)">
        <input v-model="query" class="input grow" :placeholder="`搜尋${kindLabel}（留空顯示熱門）`" />
        <button class="btn sm" type="submit" :disabled="searching"><AppIcon name="search" :size="15" /> 搜尋</button>
      </form>
      <p class="muted small">只列出支援 {{ server.typeName }} {{ server.mcVersion }} 的項目，必要的相依項目會一併安裝。</p>

      <div v-if="hits.length" class="hits">
        <article v-for="h in hits" :key="h.projectId" class="card hit">
          <img v-if="h.iconUrl" :src="h.iconUrl" alt="" class="icon" loading="lazy" referrerpolicy="no-referrer" />
          <div v-else class="icon ph"><AppIcon name="puzzle" :size="22" /></div>
          <div class="txt">
            <strong>{{ h.title }}</strong>
            <span class="muted small">by {{ h.author }} · {{ formatNumber(h.downloads) }} 次下載</span>
            <p class="small">{{ h.description }}</p>
          </div>
          <button class="btn sm soft" :disabled="task.active" @click="install(h)"><AppIcon name="download" :size="14" /> 安裝</button>
        </article>
      </div>
      <div v-else-if="searched && !searching" class="card"><EmptyState icon="search" title="找不到符合的項目" hint="換個關鍵字試試。" /></div>
      <div v-if="hits.length < total" class="row" style="justify-content:center">
        <button class="btn ghost sm" :disabled="searching" @click="search(true)">載入更多</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.seg { display: inline-flex; padding: 4px; gap: 4px; background: var(--accent-soft); border-radius: 12px; align-self: flex-start; }
.seg button { border: 0; background: transparent; padding: 7px 18px; border-radius: 9px; font-weight: 600; color: var(--muted); cursor: pointer; }
.seg button.on { background: var(--surface); color: var(--accent); box-shadow: 0 1px 3px rgba(0, 0, 0, .08); }
.head { padding: 14px 20px 8px; }
.prog { padding: 4px 20px 12px; }
tr.off td { opacity: .55; }
.grow { flex: 1; min-width: 200px; }
.hits { display: grid; gap: 12px; }
.hit { display: flex; align-items: center; gap: 16px; padding: 14px 18px; }
.icon { width: 52px; height: 52px; border-radius: 12px; object-fit: cover; flex: none; background: var(--paper); }
.icon.ph { display: grid; place-items: center; color: var(--muted); }
.txt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.txt p { color: var(--muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
@media (max-width: 560px) { .hit { flex-wrap: wrap; } }
</style>
