<script setup>
import { computed } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import McStatusBadge from './McStatusBadge.vue'
import { usePower } from '../composables/usePower'
import { isAlive } from '../constants'

const props = defineProps({ server: { type: Object, required: true } })
const emit = defineEmits(['changed'])

const { busy, act } = usePower(() => props.server, () => emit('changed'))
const alive = computed(() => isAlive(props.server))
const ready = computed(() => props.server.installState === 'READY')
const players = computed(() => props.server.runtime?.players?.length || 0)
</script>

<template>
  <article class="card srv">
    <RouterLink :to="{ name: 'mc-server', params: { id: server.id } }" class="head">
      <span class="ico"><AppIcon name="server" :size="20" /></span>
      <div class="titles">
        <strong>{{ server.name }}</strong>
        <span class="muted small">{{ server.typeName }} {{ server.mcVersion }}</span>
      </div>
      <McStatusBadge :server="server" />
    </RouterLink>

    <div class="meta small muted">
      <span><AppIcon name="globe" :size="14" /> :{{ server.port }}</span>
      <span><AppIcon name="memory" :size="14" /> {{ (server.memoryMb / 1024).toFixed(server.memoryMb % 1024 ? 1 : 0) }} GB</span>
      <span v-if="server.runtime?.state === 'RUNNING'"><AppIcon name="users" :size="14" /> {{ players }} 人</span>
    </div>
    <p v-if="server.description" class="desc small muted">{{ server.description }}</p>

    <div class="row tight actions">
      <button v-if="!alive" class="btn sm" :disabled="busy || !ready" @click="act('start')">
        <AppIcon name="play" :size="14" /> 啟動
      </button>
      <button v-else class="btn sm ghost" :disabled="busy || server.runtime.state === 'STOPPING'" @click="act('stop')">
        <AppIcon name="stop" :size="14" /> 停止
      </button>
      <div class="spacer" />
      <RouterLink :to="{ name: 'mc-server', params: { id: server.id } }" class="btn sm ghost">管理</RouterLink>
    </div>
  </article>
</template>

<style scoped>
.srv { padding: 18px 20px; display: flex; flex-direction: column; gap: 12px; transition: border-color .15s, box-shadow .15s; }
.srv:hover { border-color: var(--accent); box-shadow: 0 8px 24px rgba(14, 124, 102, .1); }
.head { display: flex; align-items: center; gap: 12px; min-width: 0; }
.ico { width: 40px; height: 40px; border-radius: 12px; background: var(--accent-soft); color: var(--accent); display: grid; place-items: center; flex: none; }
.titles { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.titles strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { display: flex; gap: 16px; flex-wrap: wrap; }
.meta span { display: inline-flex; align-items: center; gap: 4px; }
.desc { overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.actions { margin-top: 2px; }
</style>
