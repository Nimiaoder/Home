<script setup>
defineProps({ modelValue: Boolean, disabled: Boolean })
defineEmits(['update:modelValue'])
</script>

<template>
  <button type="button" role="switch" :aria-checked="modelValue" :disabled="disabled"
          :class="['sw', { on: modelValue }]" @click="$emit('update:modelValue', !modelValue)">
    <span />
  </button>
</template>

<style scoped>
.sw { width: 40px; height: 22px; border-radius: 999px; border: 0; padding: 2px; background: #cfd9d7; cursor: pointer; transition: background .15s; flex: none; }
.sw span { display: block; width: 18px; height: 18px; border-radius: 50%; background: #fff; transition: transform .15s; box-shadow: 0 1px 3px rgba(0,0,0,.25); }
.sw.on { background: var(--accent); }
.sw.on span { transform: translateX(18px); }
.sw:disabled { opacity: .5; cursor: not-allowed; }
</style>
