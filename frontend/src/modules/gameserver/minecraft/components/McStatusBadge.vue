<script setup>
import { computed } from 'vue'
import { statusOf } from '../constants'

const props = defineProps({ server: Object })
const info = computed(() => statusOf(props.server))
</script>

<template>
  <span :class="['tag', info.tone]">
    <i class="dot" :style="{ background: info.dot }" />{{ info.label }}
  </span>
</template>

<style scoped>
.dot { width: 7px; height: 7px; border-radius: 50%; display: inline-block; }
</style>
