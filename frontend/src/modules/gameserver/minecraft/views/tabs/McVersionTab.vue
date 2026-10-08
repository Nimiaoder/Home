<script setup>
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import VersionPicker from '../../components/VersionPicker.vue'
import JavaRequirementNotice from '../../components/JavaRequirementNotice.vue'
import { useServer } from '../../composables/useServerContext'
import { useTaskRunner } from '@/composables/useTask'
import { mcServerApi } from '@/api/minecraft'
import { logger } from '@/utils/logger'
import { confirmDialog } from '@/utils/confirm'
import { formatDate } from '@/utils/format'
import { isAlive } from '../../constants'
import { toast } from '@/utils/toast'

const { server, id, reload } = useServer()
const alive = computed(() => isAlive(server.value))
const installing = computed(() => server.value.installState === 'INSTALLING')

const pick = ref({ type: server.value.type, mcVersion: server.value.mcVersion, build: server.value.build || '' })
const requiredJava = ref(0)
const backupFirst = ref(true)
const history = ref([])
const log = logger('McVersionTab')
const { state: task, track } = useTaskRunner()

const loadHistory = () => mcServerApi.history(id.value, (d) => (history.value = d))
onMounted(() => {
  log.debug('*****McVersionTab*****')
  loadHistory()
})

const unchanged = computed(() =>
  pick.value.type === server.value.type && pick.value.mcVersion === server.value.mcVersion &&
  (!pick.value.build || pick.value.build === (server.value.build || '')))

// 簡易數字比較，用來提示「降級」
function cmp(a, b) {
  const x = (a.match(/^\d+(\.\d+)*/)?.[0] || '0').split('.').map(Number)
  const y = (b.match(/^\d+(\.\d+)*/)?.[0] || '0').split('.').map(Number)
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    const d = (x[i] || 0) - (y[i] || 0)
    if (d) return d
  }
  return 0
}

async function apply() {
  const downgrade = cmp(pick.value.mcVersion, server.value.mcVersion) < 0
  const typeChange = pick.value.type !== server.value.type
  const notes = []
  if (downgrade) notes.push('這是降級：較新版本建立的地圖，在舊版可能無法載入甚至損壞。')
  if (typeChange) notes.push(`類型將從 ${server.value.typeName} 改為 ${pick.value.type}：原本的模組 / 插件不一定相容。`)
  if (!backupFirst.value) notes.push('你沒有勾選「先備份」。')
  const { ok } = await confirmDialog({
    title: '切換版本',
    message: `${server.value.typeName} ${server.value.mcVersion} → ${pick.value.type} ${pick.value.mcVersion}\n\n${notes.join('\n') || '地圖與設定都會保留，只替換伺服器核心。'}`,
    confirmText: '開始切換', danger: downgrade
  })
  if (!ok) return
  log.info('切換版本', pick.value.type, pick.value.mcVersion, pick.value.build)
  mcServerApi.changeVersion(id.value, { ...pick.value, backupFirst: backupFirst.value }, (d) => {
    reload()
    track(d.taskId, { title: '切換版本', onDone: () => { toast.success('版本已切換'); reload(); loadHistory() }, onFail: reload })
  })
}
</script>

<template>
  <div class="stack">
    <section class="card card-pad stack">
      <h3 class="section-title" style="margin:0">目前版本</h3>
      <div class="row">
        <span class="tag info">{{ server.typeName }}</span>
        <strong class="big">Minecraft {{ server.mcVersion }}</strong>
        <span v-if="server.build && server.build !== 'default'" class="muted small mono">{{ server.build }}</span>
      </div>
    </section>

    <section class="card card-pad stack">
      <h3 class="section-title" style="margin:0">切換版本</h3>
      <div v-if="alive" class="notice warn">請先停止伺服器再切換版本。</div>
      <VersionPicker v-model="pick" @java="requiredJava = $event" />
      <JavaRequirementNotice :required="requiredJava" :java-path="server.javaPath" />
      <label class="check"><input v-model="backupFirst" type="checkbox" /> 切換前先備份目前使用中的地圖（建議）</label>
      <ProgressBar v-if="task.active" :percent="task.percent" :done="task.bytesDone" :total="task.bytesTotal" :speed="task.speed" :label="task.message || task.title" />
      <div class="row">
        <button class="btn sm" :disabled="alive || installing || task.active || unchanged || !pick.mcVersion" @click="apply">
          <AppIcon name="tag" :size="15" /> 套用版本
        </button>
        <RouterLink :to="{ name: 'mc-library' }" class="small muted lnk">管理版本庫 →</RouterLink>
      </div>
      <p class="muted small">核心會先下載到版本庫再複製到此伺服器，地圖、設定與模組不受影響。</p>
    </section>

    <section class="card">
      <div class="head"><h3 class="section-title" style="margin:0">版本紀錄</h3></div>
      <p v-if="!history.length" class="muted small pad">還沒有紀錄。</p>
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>時間</th><th>動作</th><th>版本</th></tr></thead>
          <tbody>
            <tr v-for="(h, i) in history" :key="i">
              <td class="muted small">{{ formatDate(h.createdAt) }}</td>
              <td><span :class="['tag', h.action === 'INSTALL' ? 'ok' : 'info']">{{ h.action === 'INSTALL' ? '首次安裝' : '切換版本' }}</span></td>
              <td>{{ h.type }} {{ h.mcVersion }} <span v-if="h.build && h.build !== 'default'" class="muted small mono">{{ h.build }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.big { font-size: 18px; }
.head { padding: 16px 20px 4px; }
.pad { padding: 8px 20px 18px; }
.lnk:hover { color: var(--accent); }
</style>
