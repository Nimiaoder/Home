<script setup>
import { toastState, remove } from '@/utils/toast'
</script>

<template>
  <div class="toast-host" aria-live="polite">
    <TransitionGroup name="toast">
      <button v-for="t in toastState.list" :key="t.id" :class="['toast', t.type]" @click="remove(t.id)">
        <span class="dot" />
        {{ t.message }}
      </button>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
  display: flex; flex-direction: column; gap: 10px; z-index: 100; width: min(92vw, 420px);
}
.toast {
  display: flex; align-items: center; gap: 10px; text-align: left;
  padding: 12px 16px; border: 1px solid var(--line); border-radius: 12px;
  background: var(--surface); color: var(--ink); font-size: 14px; cursor: pointer;
  box-shadow: 0 8px 28px rgba(15, 42, 46, 0.14);
}
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--muted); flex: none; }
.success .dot { background: var(--accent); }
.error { border-color: #f1c4bd; }
.error .dot { background: var(--danger); }
.toast-enter-active, .toast-leave-active { transition: opacity .2s, transform .2s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
