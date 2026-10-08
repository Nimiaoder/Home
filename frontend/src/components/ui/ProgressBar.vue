<script setup>
import { computed } from 'vue'
import { formatBytes, formatSpeed } from '@/utils/format'

// percent：0~100；-1 代表不確定（顯示流動動畫）
// done / total / speed：有下載量時，在進度條下方顯示「已下載 / 總大小」與下載速率
const props = defineProps({
  percent: { type: Number, default: -1 },
  label: String,
  done: { type: Number, default: 0 },
  total: { type: Number, default: -1 },
  speed: { type: Number, default: 0 }
})

const sizeText = computed(() => {
  if (!props.done && props.total <= 0) return ''
  return props.total > 0 ? `${formatBytes(props.done)} / ${formatBytes(props.total)}` : formatBytes(props.done)
})
const speedText = computed(() => formatSpeed(props.speed))
</script>

<template>
  <div class="progress">
    <div v-if="label" class="label">{{ label }}</div>
    <div class="track">
      <div :class="['bar', { indeterminate: percent < 0 }]" :style="percent >= 0 ? { width: percent + '%' } : null" />
    </div>
    <div v-if="sizeText || speedText" class="detail">
      <span>{{ sizeText }}</span>
      <span>{{ speedText }}</span>
    </div>
  </div>
</template>

<style scoped>
.label { font-size: 13px; color: var(--muted); margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.track { height: 8px; border-radius: 999px; background: var(--accent-soft); overflow: hidden; position: relative; }
.bar { height: 100%; border-radius: 999px; background: var(--accent); transition: width .3s; }
.bar.indeterminate { position: absolute; width: 35%; animation: slide 1.2s ease-in-out infinite; }
.detail { display: flex; justify-content: space-between; gap: 12px; margin-top: 6px; font-size: 12px; color: var(--muted); font-variant-numeric: tabular-nums; }
@keyframes slide { 0% { left: -35%; } 100% { left: 100%; } }
</style>
