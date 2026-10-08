<script setup>
import { onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  title: String,
  width: { type: String, default: '520px' },
  persistent: Boolean // true：點背景或按 Esc 不會關閉（例如正在送出）
})
const emit = defineEmits(['update:modelValue', 'close'])

function close() {
  if (props.persistent) return
  emit('update:modelValue', false)
  emit('close')
}
function onKey(e) {
  if (e.key === 'Escape' && props.modelValue) close()
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="mask" @mousedown.self="close">
        <div class="dialog" :style="{ width }" role="dialog" aria-modal="true">
          <header v-if="title || $slots.header">
            <slot name="header"><h2>{{ title }}</h2></slot>
          </header>
          <div class="body"><slot /></div>
          <footer v-if="$slots.footer"><slot name="footer" /></footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mask {
  position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 16px;
  background: rgba(15, 42, 46, .45); backdrop-filter: blur(2px);
}
.dialog {
  max-width: 100%; max-height: calc(100vh - 32px); display: flex; flex-direction: column;
  background: var(--surface); border-radius: 18px; box-shadow: 0 24px 60px rgba(15, 42, 46, .3);
}
header { padding: 20px 24px 0; }
header h2 { font-size: 18px; }
.body { padding: 18px 24px; overflow-y: auto; }
footer { padding: 0 24px 20px; display: flex; justify-content: flex-end; gap: 10px; }
.modal-enter-active, .modal-leave-active { transition: opacity .18s; }
.modal-enter-active .dialog, .modal-leave-active .dialog { transition: transform .18s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .dialog, .modal-leave-to .dialog { transform: translateY(10px) scale(.98); }
</style>
