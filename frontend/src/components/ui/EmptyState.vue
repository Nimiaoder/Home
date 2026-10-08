<script setup>
import AppIcon from './AppIcon.vue'
defineProps({ icon: { type: String, default: 'package' }, title: String, hint: String })
</script>

<template>
  <div class="empty">
    <AppIcon :name="icon" :size="30" />
    <strong>{{ title }}</strong>
    <p v-if="hint">{{ hint }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 44px 16px; text-align: center; color: var(--muted); }
.empty strong { color: var(--ink); }
.empty p { font-size: 14px; max-width: 420px; }
</style>
