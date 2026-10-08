<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SwitchToggle from '@/components/ui/SwitchToggle.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import { useServer } from '../../composables/useServerContext'
import { mcResourceApi, mcServerApi } from '@/api/minecraft'
import { confirmDialog } from '@/utils/confirm'
import { toast } from '@/utils/toast'
import { COMMON_PROPERTIES, MANAGED_KEYS } from '../../propertySchema'
import { G1_FLAGS, isAlive } from '../../constants'

const router = useRouter()
const { server, id, reload } = useServer()
const alive = computed(() => isAlive(server.value))

// ---------------- 一般設定 ----------------
const s = server.value
const form = reactive({
  name: s.name, description: s.description || '', port: s.port, memoryGb: s.memoryMb / 1024,
  javaPath: s.javaPath || '', jvmArgs: s.jvmArgs || '', autoStart: s.autoStart
})
const javaList = ref([])
const saving = ref(false)

const javaOptions = computed(() => {
  const opts = javaList.value.map((j) => ({ value: j.path, label: `Java ${j.major}　${j.path}` }))
  if (form.javaPath && !opts.some((o) => o.value === form.javaPath)) opts.push({ value: form.javaPath, label: form.javaPath })
  return opts
})

function saveGeneral() {
  if (!form.name.trim()) return toast.error('請輸入名稱')
  saving.value = true
  mcServerApi.update(id.value, {
    name: form.name.trim(), description: form.description.trim(), port: form.port,
    memoryMb: Math.round(form.memoryGb * 1024), javaPath: form.javaPath, jvmArgs: form.jvmArgs.trim(), autoStart: form.autoStart
  }, reload, { showSuccess: true, onFinally: () => (saving.value = false) })
}

// ---------------- server.properties ----------------
const props = ref({})          // 後端目前的內容
const edits = reactive({})     // 使用者改過的 key → value
const loaded = ref(false)
const newKey = ref('')
const newValue = ref('')
const savingProps = ref(false)

function loadProps() {
  mcServerApi.properties(id.value, (d) => {
    props.value = d
    Object.keys(edits).forEach((k) => delete edits[k])
    loaded.value = true
  })
}
const hasProps = computed(() => Object.keys(props.value).length > 0)
const current = (p) => edits[p.key] ?? props.value[p.key] ?? p.def
function setProp(key, value) {
  if (String(value) === (props.value[key] ?? '')) delete edits[key]
  else edits[key] = String(value)
}
const dirty = computed(() => Object.keys(edits).length)

const commonKeys = new Set(COMMON_PROPERTIES.map((p) => p.key))
const advanced = computed(() => Object.keys(props.value).filter((k) => !commonKeys.has(k) && !(k in MANAGED_KEYS)))

function addCustom() {
  const k = newKey.value.trim()
  if (!/^[A-Za-z0-9._-]+$/.test(k)) return toast.error('設定名稱只能使用英數字與 . _ -')
  edits[k] = newValue.value
  props.value = { ...props.value, [k]: props.value[k] ?? '' }
  newKey.value = ''
  newValue.value = ''
}

function saveProps() {
  savingProps.value = true
  mcServerApi.saveProperties(id.value, { ...edits }, loadProps, { showSuccess: true, onFinally: () => (savingProps.value = false) })
}

onMounted(() => {
  mcResourceApi.javaList(false, (d) => (javaList.value = d), { showError: false })
  loadProps()
})

// ---------------- 刪除 ----------------
async function removeServer() {
  const { ok, checked } = await confirmDialog({
    title: `刪除伺服器「${server.value.name}」`,
    message: '伺服器會從清單移除。此動作無法復原。',
    checkbox: '同時刪除所有檔案（地圖、模組、設定與備份）',
    confirmText: '刪除', danger: true
  })
  if (!ok) return
  mcServerApi.remove(id.value, checked, () => router.replace({ name: 'mc-servers' }), { showSuccess: true })
}
</script>

<template>
  <div class="stack">
    <div v-if="alive" class="notice warn">伺服器執行中：連接埠、記憶體、Java 與 server.properties 的變更需要重新啟動後才會生效。</div>

    <section class="card card-pad stack">
      <h3 class="section-title" style="margin:0">一般</h3>
      <div class="form-grid">
        <div class="form-field"><label>名稱</label><input v-model="form.name" class="input" maxlength="50" /></div>
        <div class="form-field"><label>連接埠</label><input v-model.number="form.port" class="input" type="number" min="1024" max="65535" /></div>
        <div class="form-field">
          <label>記憶體上限（GB）</label>
          <input v-model.number="form.memoryGb" class="input" type="number" min="0.5" step="0.5" />
        </div>
        <div class="form-field">
          <label>Java</label>
          <select v-model="form.javaPath" class="select">
            <option value="">自動選擇（依版本）</option>
            <option v-for="o in javaOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-field"><label>說明</label><input v-model="form.description" class="input" maxlength="255" /></div>
      <div class="form-field">
        <label>額外 JVM 參數</label>
        <textarea v-model="form.jvmArgs" class="textarea mono" rows="3" placeholder="例如 -XX:+UseG1GC" />
        <div class="row tight">
          <button class="btn ghost sm" type="button" @click="form.jvmArgs = G1_FLAGS">套用建議參數 (G1GC)</button>
          <button v-if="form.jvmArgs" class="btn ghost sm" type="button" @click="form.jvmArgs = ''">清空</button>
        </div>
        <span class="hint">記憶體（-Xms / -Xmx）已由上面的設定帶入，不用再填。</span>
      </div>
      <label class="row tight check"><SwitchToggle v-model="form.autoStart" /> 後端啟動時自動啟動這個伺服器</label>
      <div><button class="btn sm" :disabled="saving" @click="saveGeneral">{{ saving ? '儲存中…' : '儲存' }}</button></div>
    </section>

    <section class="card card-pad stack">
      <div class="row">
        <h3 class="section-title" style="margin:0">遊戲設定（server.properties）</h3>
        <div class="spacer" />
        <button class="btn sm" :disabled="!dirty || savingProps" @click="saveProps">{{ savingProps ? '儲存中…' : `儲存變更${dirty ? `（${dirty}）` : ''}` }}</button>
      </div>
      <div v-if="loaded && !hasProps" class="notice small">伺服器第一次啟動後才會產生完整的設定檔。現在修改的值會先寫入，啟動時生效。</div>

      <div class="form-grid">
        <div v-for="p in COMMON_PROPERTIES" :key="p.key" class="form-field">
          <label :for="'p-' + p.key">{{ p.label }}</label>
          <template v-if="p.type === 'bool'">
            <div><SwitchToggle :model-value="current(p) === 'true'" @update:model-value="setProp(p.key, $event)" /></div>
          </template>
          <select v-else-if="p.type === 'select'" :id="'p-' + p.key" class="select" :value="current(p)" @change="setProp(p.key, $event.target.value)">
            <option v-for="o in p.options" :key="o[0]" :value="o[0]">{{ o[1] }}</option>
          </select>
          <input v-else :id="'p-' + p.key" class="input" :type="p.type === 'number' ? 'number' : 'text'" :value="current(p)"
                 @input="setProp(p.key, $event.target.value)" />
          <span v-if="p.hint" class="hint">{{ p.hint }}</span>
        </div>
      </div>

      <details v-if="hasProps || Object.keys(edits).length" class="adv">
        <summary>進階：全部屬性（{{ advanced.length }}）</summary>
        <div class="adv-list">
          <div v-for="k in advanced" :key="k" class="adv-row">
            <code class="mono small">{{ k }}</code>
            <input class="input" :value="edits[k] ?? props[k]" @input="setProp(k, $event.target.value)" />
          </div>
          <div class="adv-row">
            <input v-model="newKey" class="input mono" placeholder="新增設定名稱" />
            <div class="row tight" style="flex-wrap:nowrap">
              <input v-model="newValue" class="input" placeholder="值" />
              <button class="btn sm soft" type="button" @click="addCustom"><AppIcon name="plus" :size="14" /></button>
            </div>
          </div>
          <p class="muted small">連接埠與使用中的地圖由系統管理，不在這裡編輯。</p>
        </div>
      </details>
    </section>

    <section class="card card-pad danger">
      <h3 class="section-title">危險區域</h3>
      <div class="row">
        <p class="muted small" style="flex:1">刪除這個伺服器。需先停止伺服器。</p>
        <button class="btn ghost danger sm" :disabled="alive" @click="removeServer"><AppIcon name="trash" :size="15" /> 刪除伺服器</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.adv { border-top: 1px solid var(--line); padding-top: 14px; }
.adv summary { cursor: pointer; font-weight: 600; font-size: 14px; }
.adv-list { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }
.adv-row { display: grid; grid-template-columns: minmax(140px, 220px) 1fr; gap: 12px; align-items: center; }
.danger { border-color: #efc9c1; }
@media (max-width: 560px) { .adv-row { grid-template-columns: 1fr; gap: 4px; } }
</style>
