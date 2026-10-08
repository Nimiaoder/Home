<script setup>
// percent：0~100；-1 代表不確定（顯示流動動畫）
defineProps({ percent: { type: Number, default: -1 }, label: String })
</script>

<template>
  <div class="progress">
    <div v-if="label" class="label">{{ label }}</div>
    <div class="track">
      <div :class="['bar', { indeterminate: percent < 0 }]" :style="percent >= 0 ? { width: percent + '%' } : null" />
    </div>
  </div>
</template>

<style scoped>
.label { font-size: 13px; color: var(--muted); margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.track { height: 8px; border-radius: 999px; background: var(--accent-soft); overflow: hidden; position: relative; }
.bar { height: 100%; border-radius: 999px; background: var(--accent); transition: width .3s; }
.bar.indeterminate { position: absolute; width: 35%; animation: slide 1.2s ease-in-out infinite; }
@keyframes slide { 0% { left: -35%; } 100% { left: 100%; } }
</style>
