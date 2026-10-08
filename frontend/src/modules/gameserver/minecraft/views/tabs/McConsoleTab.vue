<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import { useServer } from '../../composables/useServerContext'
import { useConsoleStream } from '../../composables/useConsoleStream'
import { mcServerApi } from '@/api/minecraft'
import { downloadByTicket } from '@/utils/download'
import { isAlive, QUICK_COMMANDS } from '../../constants'

const { server, id, reload } = useServer()
const { lines, status, connected, fatal, clear } = useConsoleStream(() => id.value)

// 串流推來的狀態比輪詢更即時；有新狀態就順便更新外框
watch(status, () => reload())

const alive = computed(() => isAlive(server.value))
const box = ref(null)
const stick = ref(true) // 使用者捲到最底時，新訊息自動跟隨
const input = ref('')
const history = []
let hIndex = -1

function onScroll() {
  const el = box.value
  stick.value = el.scrollTop + el.clientHeight >= el.scrollHeight - 40
}
function toBottom() {
  const el = box.value
  if (el) el.scrollTop = el.scrollHeight
}
watch(lines, () => { if (stick.value) nextTick(toBottom) })

function cls(t) {
  if (t.startsWith('>')) return 'echo'
  if (t.startsWith('[Dev Console]') || t.startsWith('────') || t.startsWith('$ ')) return 'sys'
  if (/\/(ERROR|FATAL)\]/.test(t) || /^\s*(Caused by|Exception|java\.|\w+\.\w+Exception)/.test(t)) return 'err'
  if (/\/WARN\]/.test(t)) return 'warn'
  if (/^\s+at\s/.test(t)) return 'dim'
  return ''
}

function send(cmd) {
  const c = (cmd ?? input.value).trim()
  if (!c) return
  mcServerApi.command(id.value, c, () => {
    if (history[0] !== c) history.unshift(c)
    hIndex = -1
    if (cmd === undefined) input.value = ''
    stick.value = true
  })
}
function onKey(e) {
  if (e.key === 'ArrowUp' && history.length) {
    e.preventDefault()
    hIndex = Math.min(hIndex + 1, history.length - 1)
    input.value = history[hIndex]
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    hIndex = Math.max(hIndex - 1, -1)
    input.value = hIndex < 0 ? '' : history[hIndex]
  }
}
function downloadLog() {
  mcServerApi.downloadLog(id.value, downloadByTicket)
}
</script>

<template>
  <div class="console card">
    <div class="bar row tight">
      <span :class="['dot', { on: connected }]" />
      <span class="small">{{ connected ? '即時連線中' : '重新連線中…' }}</span>
      <div class="spacer" />
      <button class="btn sm ghost light" @click="clear"><AppIcon name="trash" :size="14" /> 清除畫面</button>
      <button class="btn sm ghost light" @click="downloadLog"><AppIcon name="download" :size="14" /> latest.log</button>
    </div>

    <div v-if="fatal" class="fatal">{{ fatal }}</div>
    <div ref="box" class="screen mono" @scroll="onScroll">
      <div v-if="!lines.length" class="dim">（尚無輸出。啟動伺服器後，訊息會即時顯示在這裡。）</div>
      <div v-for="l in lines" :key="l.seq" :class="['ln', cls(l.text)]">{{ l.text }}</div>
    </div>
    <button v-if="!stick" class="jump btn sm" @click="stick = true; toBottom()">跳到最新 ↓</button>

    <div class="quick row tight">
      <button v-for="c in QUICK_COMMANDS" :key="c" class="chip mono" :disabled="!alive" @click="send(c)">{{ c }}</button>
    </div>
    <form class="cmd" @submit.prevent="send()">
      <span class="prompt mono">&gt;</span>
      <input v-model="input" class="mono" :disabled="!alive" :placeholder="alive ? '輸入指令（不用加 /），Enter 送出，↑↓ 切換歷史' : '伺服器未在執行'"
             autocomplete="off" spellcheck="false" @keydown="onKey" />
      <button class="btn sm" type="submit" :disabled="!alive || !input.trim()">送出</button>
    </form>
  </div>
</template>

<style scoped>
.console { position: relative; background: var(--term-bg); border-color: #163036; color: var(--term-ink); overflow: hidden; }
.bar { padding: 10px 14px; border-bottom: 1px solid #1d3a41; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #c8452f; }
.dot.on { background: #31d0a2; box-shadow: 0 0 8px #31d0a2; }
.btn.light { color: var(--term-ink); border-color: #284850; }
.btn.light:hover:not(:disabled) { background: #163036; }
.screen { height: min(62vh, 560px); overflow-y: auto; padding: 12px 16px; font-size: 13px; line-height: 1.55; }
.ln { white-space: pre-wrap; word-break: break-all; }
.ln.warn { color: #f0c05a; }
.ln.err { color: #ff8f7e; }
.ln.sys { color: #62d6b4; }
.ln.echo { color: #8ec5ff; }
.ln.dim, .dim { color: #6b8a8a; }
.fatal { padding: 10px 16px; background: #4a1d17; color: #ffb4a8; font-size: 14px; }
.jump { position: absolute; right: 18px; bottom: 112px; box-shadow: 0 4px 14px rgba(0, 0, 0, .4); }
.quick { padding: 8px 14px 0; }
.chip { border: 1px solid #284850; background: transparent; color: var(--term-ink); border-radius: 999px; padding: 3px 12px; font-size: 12px; cursor: pointer; }
.chip:hover:not(:disabled) { background: #163036; }
.chip:disabled { opacity: .4; cursor: not-allowed; }
.cmd { display: flex; align-items: center; gap: 10px; padding: 10px 14px 14px; }
.prompt { color: #62d6b4; }
.cmd input { flex: 1; min-width: 0; height: 36px; border: 1px solid #284850; border-radius: 10px; background: #0a191c; color: #fff; padding: 0 12px; font-size: 13px; }
.cmd input:focus { outline: none; border-color: #31d0a2; }
.cmd input:disabled { opacity: .5; }
</style>
