<script setup>
import { computed } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import JavaRequirementNotice from '../../components/JavaRequirementNotice.vue'
import { useServer } from '../../composables/useServerContext'
import { mcServerApi } from '@/api/minecraft'
import { formatDuration } from '@/utils/format'
import { toast } from '@/utils/toast'
import { statusOf } from '../../constants'

const { server, reload } = useServer()
const rt = computed(() => server.value.runtime)
const running = computed(() => rt.value.state !== 'STOPPED' && rt.value.state !== 'CRASHED')
const address = computed(() => `${location.hostname}:${server.value.port}`)
const status = computed(() => statusOf(server.value))

function acceptEula() {
  mcServerApi.acceptEula(server.value.id, reload, { showSuccess: true })
}
async function copy() {
  try {
    await navigator.clipboard.writeText(address.value)
    toast.success('已複製連線位址')
  } catch {
    toast.info(address.value)
  }
}
</script>

<template>
  <div class="stack">
    <div v-if="!server.eulaAccepted" class="notice warn row">
      <span>啟動前需要同意 <a href="https://aka.ms/MinecraftEULA" target="_blank" rel="noopener" class="lnk">Minecraft EULA</a>。</span>
      <div class="spacer" />
      <button class="btn sm" @click="acceptEula">我同意</button>
    </div>
    <div v-if="rt.state === 'CRASHED'" class="notice err">
      伺服器非預期結束（exit code {{ rt.exitCode }}）。請到「主控台」查看最後的訊息。
    </div>

    <div class="stats">
      <div class="card stat">
        <span class="muted small"><AppIcon name="power" :size="14" /> 狀態</span>
        <strong>{{ status.label }}</strong>
      </div>
      <div class="card stat">
        <span class="muted small"><AppIcon name="clock" :size="14" /> 運行時間</span>
        <strong>{{ running ? formatDuration(rt.uptimeSeconds) : '-' }}</strong>
      </div>
      <div class="card stat">
        <span class="muted small"><AppIcon name="memory" :size="14" /> 記憶體</span>
        <strong>{{ rt.memoryMb != null ? rt.memoryMb + ' MB' : '-' }}</strong>
        <span class="muted small">上限 {{ server.memoryMb }} MB</span>
      </div>
      <div class="card stat">
        <span class="muted small"><AppIcon name="users" :size="14" /> 線上玩家</span>
        <strong>{{ running ? rt.players.length : '-' }}</strong>
      </div>
    </div>

    <section v-if="rt.players.length" class="card card-pad">
      <h3 class="section-title">線上玩家</h3>
      <div class="row tight"><span v-for="p in rt.players" :key="p" class="tag ok">{{ p }}</span></div>
    </section>

    <section class="card card-pad stack">
      <h3 class="section-title" style="margin:0">伺服器資訊</h3>
      <dl class="info">
        <dt>連線位址</dt>
        <dd><span class="mono">{{ address }}</span> <button class="btn ghost sm" @click="copy">複製</button>
          <span class="muted small">（內網位址；外網請設定連接埠轉發）</span></dd>
        <dt>類型 / 版本</dt>
        <dd>{{ server.typeName }} · Minecraft {{ server.mcVersion }}<span v-if="server.build && server.build !== 'default'" class="muted"> · {{ server.build }}</span></dd>
        <dt>記憶體上限</dt>
        <dd>{{ server.memoryMb }} MB</dd>
        <dt>資料夾</dt>
        <dd class="mono small">{{ server.dirPath }}</dd>
      </dl>
      <JavaRequirementNotice :required="server.requiredJava" :java-path="server.javaPath" />
    </section>
  </div>
</template>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 14px; }
.stat { padding: 16px 18px; display: flex; flex-direction: column; gap: 4px; }
.stat span { display: inline-flex; align-items: center; gap: 6px; }
.stat strong { font-size: 22px; }
.info { display: grid; grid-template-columns: 110px 1fr; gap: 10px 16px; margin: 0; font-size: 14px; }
.info dt { color: var(--muted); }
.info dd { margin: 0; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; min-width: 0; word-break: break-all; }
.lnk { text-decoration: underline; font-weight: 600; }
@media (max-width: 560px) { .info { grid-template-columns: 1fr; gap: 2px; } .info dd { margin-bottom: 10px; } }
</style>
