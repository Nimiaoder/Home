<script setup>
import { computed, onMounted, ref } from 'vue'
import { mcResourceApi } from '@/api/minecraft'

// 顯示「這個版本需要 Java N」，並檢查系統上有沒有可用的 Java
const props = defineProps({ required: Number, javaPath: String })
const list = ref(null)

onMounted(() => mcResourceApi.javaList(false, (d) => (list.value = d), { showError: false }))

const ok = computed(() => !list.value || !props.required || !!props.javaPath || list.value.some((j) => j.major >= props.required))
</script>

<template>
  <div v-if="required" :class="['notice small', ok ? 'ok' : 'warn']">
    <template v-if="ok">此版本需要 Java {{ required }} 以上。</template>
    <template v-else>
      此版本需要 Java {{ required }} 以上，但系統上找不到。請先到
      <RouterLink :to="{ name: 'mc-library', query: { tab: 'java' } }" class="lnk">資源庫 → Java 環境</RouterLink>
      一鍵下載安裝。
    </template>
  </div>
</template>

<style scoped>
.lnk { text-decoration: underline; font-weight: 600; }
</style>
