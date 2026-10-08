<script setup>
import AppIcon from './AppIcon.vue'
defineProps({ title: String, subtitle: String, backTo: [String, Object], backLabel: { type: String, default: '返回' } })
</script>

<template>
  <header class="ph">
    <RouterLink v-if="backTo" :to="backTo" class="back"><AppIcon name="back" :size="16" /> {{ backLabel }}</RouterLink>
    <div class="row">
      <div class="titles">
        <h1>{{ title }}</h1>
        <p v-if="subtitle" class="muted">{{ subtitle }}</p>
      </div>
      <div class="spacer" />
      <div class="row tight"><slot name="actions" /></div>
    </div>
  </header>
</template>

<style scoped>
.ph { margin-bottom: 24px; }
.back { display: inline-flex; align-items: center; gap: 4px; color: var(--muted); font-size: 14px; margin-bottom: 10px; }
.back:hover { color: var(--accent); }
h1 { font-size: 26px; line-height: 1.3; }
.titles { min-width: 0; }
</style>
