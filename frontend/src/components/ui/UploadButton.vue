<script setup>
import { ref } from 'vue'
import AppIcon from './AppIcon.vue'

defineProps({
  accept: String,
  label: { type: String, default: '上傳檔案' },
  disabled: Boolean,
  small: { type: Boolean, default: true }
})
const emit = defineEmits(['pick'])
const input = ref(null)

function onChange(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 允許連續選同一個檔案
  if (file) emit('pick', file)
}
</script>

<template>
  <button :class="['btn soft', { sm: small }]" :disabled="disabled" type="button" @click="input.click()">
    <AppIcon name="upload" :size="16" /> {{ label }}
  </button>
  <input ref="input" type="file" :accept="accept" hidden @change="onChange" />
</template>
