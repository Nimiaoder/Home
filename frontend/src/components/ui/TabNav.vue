<script setup>
import AppIcon from './AppIcon.vue'
// tabs: [{ to, label, icon }]
defineProps({ tabs: { type: Array, required: true } })
</script>

<template>
  <nav class="tabs">
    <RouterLink v-for="t in tabs" :key="t.label" :to="t.to" class="tab" active-class="active">
      <AppIcon v-if="t.icon" :name="t.icon" :size="16" /> {{ t.label }}
    </RouterLink>
  </nav>
</template>

<style scoped>
.tabs { display: flex; gap: 4px; overflow-x: auto; border-bottom: 1px solid var(--line); margin-bottom: 24px; scrollbar-width: none; }
.tab {
  display: inline-flex; align-items: center; gap: 6px; padding: 10px 14px; font-size: 14px; font-weight: 600;
  color: var(--muted); border-bottom: 2px solid transparent; margin-bottom: -1px; white-space: nowrap;
}
.tab:hover { color: var(--ink); }
.tab.active { color: var(--accent); border-bottom-color: var(--accent); }
</style>
