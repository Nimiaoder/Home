<script setup>
import BaseModal from '@/components/ui/BaseModal.vue'
import { confirmState, settleConfirm } from '@/utils/confirm'
</script>

<template>
  <BaseModal :model-value="confirmState.open" :title="confirmState.options.title || '請確認'" width="440px"
             @update:model-value="settleConfirm(false)">
    <p class="msg">{{ confirmState.options.message }}</p>
    <label v-if="confirmState.options.checkbox" class="check box">
      <input v-model="confirmState.checked" type="checkbox" />
      {{ confirmState.options.checkbox }}
    </label>
    <template #footer>
      <button class="btn ghost sm" @click="settleConfirm(false)">取消</button>
      <button :class="['btn sm', { danger: confirmState.options.danger }]" @click="settleConfirm(true)">
        {{ confirmState.options.confirmText || '確定' }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.msg { color: var(--ink); white-space: pre-line; }
.box { margin-top: 14px; }
</style>
