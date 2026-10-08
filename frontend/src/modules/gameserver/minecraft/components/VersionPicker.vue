<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { mcResourceApi } from '@/api/minecraft'

// 選擇「類型 → Minecraft 版本 → 建置」。v-model 是 { type, mcVersion, build }。
const props = defineProps({
  modelValue: { type: Object, required: true },
  lockType: Boolean
})
const emit = defineEmits(['update:modelValue', 'java'])

const types = ref([])
const versions = ref([])
const builds = ref([])
const showSnapshots = ref(false)
const loadingVersions = ref(false)
const loadingBuilds = ref(false)
let versionToken = 0
let buildToken = 0

const shown = computed(() => versions.value.filter((v) => showSnapshots.value || v.kind === 'release'))
const hasSnapshots = computed(() => versions.value.some((v) => v.kind !== 'release'))

function set(patch) {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

function pickDefaultBuild(list) {
  for (const ch of ['recommended', 'stable']) {
    const b = list.find((x) => x.channel === ch)
    if (b) return b.id
  }
  return list[0]?.id || ''
}

function loadVersions() {
  const token = ++versionToken
  const type = props.modelValue.type
  if (!type) return
  loadingVersions.value = true
  versions.value = []
  builds.value = []
  mcResourceApi.versions(type, (list) => {
    if (token !== versionToken) return
    versions.value = list
    const current = props.modelValue.mcVersion
    const keep = shown.value.some((v) => v.id === current) || (current && list.some((v) => v.id === current))
    if (!keep) set({ mcVersion: shown.value[0]?.id || '', build: '' })
    else loadBuilds()
  }, { onFinally: () => { if (token === versionToken) loadingVersions.value = false } })
}

function loadBuilds() {
  const token = ++buildToken
  const { type, mcVersion } = props.modelValue
  if (!type || !mcVersion) return
  loadingBuilds.value = true
  builds.value = []
  mcResourceApi.builds(type, mcVersion, (list) => {
    if (token !== buildToken) return
    builds.value = list
    if (!list.some((b) => b.id === props.modelValue.build)) set({ build: pickDefaultBuild(list) })
  }, { onFinally: () => { if (token === buildToken) loadingBuilds.value = false } })
}

watch(() => props.modelValue.type, loadVersions)
watch(() => props.modelValue.mcVersion, loadBuilds)
watch(showSnapshots, () => {
  if (!shown.value.some((v) => v.id === props.modelValue.mcVersion)) set({ mcVersion: shown.value[0]?.id || '', build: '' })
})
// 通知父層目前版本需要的 Java 版本
watch(
  () => [props.modelValue.mcVersion, versions.value],
  () => emit('java', versions.value.find((v) => v.id === props.modelValue.mcVersion)?.requiredJava || 0)
)

onMounted(() => {
  mcResourceApi.types((list) => {
    types.value = list
    if (!props.modelValue.type && list.length) set({ type: list[0].type })
  })
  loadVersions()
})
</script>

<template>
  <div class="picker">
    <div class="form-field">
      <label>伺服器類型</label>
      <select class="select" :value="modelValue.type" :disabled="lockType" @change="set({ type: $event.target.value, mcVersion: '', build: '' })">
        <option v-for="t in types" :key="t.type" :value="t.type">
          {{ t.name }}{{ t.contentKind === 'MOD' ? '（模組）' : t.contentKind === 'PLUGIN' ? '（插件）' : '' }}
        </option>
      </select>
    </div>
    <div class="form-field">
      <label>Minecraft 版本</label>
      <select class="select" :value="modelValue.mcVersion" :disabled="loadingVersions" @change="set({ mcVersion: $event.target.value, build: '' })">
        <option v-if="loadingVersions" value="">載入中…</option>
        <option v-for="v in shown" :key="v.id" :value="v.id">{{ v.id }}{{ v.kind !== 'release' ? '（預覽）' : '' }}</option>
      </select>
      <label v-if="hasSnapshots" class="check small"><input v-model="showSnapshots" type="checkbox" /> 顯示預覽 / 快照版本</label>
    </div>
    <div v-if="builds.length > 1 || loadingBuilds" class="form-field">
      <label>建置 / 載入器版本</label>
      <select class="select" :value="modelValue.build" :disabled="loadingBuilds" @change="set({ build: $event.target.value })">
        <option v-if="loadingBuilds" value="">載入中…</option>
        <option v-for="b in builds" :key="b.id" :value="b.id">{{ b.label }}</option>
      </select>
    </div>
  </div>
</template>

<style scoped>
.picker { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px 16px; }
</style>
