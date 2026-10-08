<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import McServerCard from '../components/McServerCard.vue'
import McCreateServerDialog from '../components/McCreateServerDialog.vue'
import { mcServerApi } from '@/api/minecraft'
import { usePolling } from '@/composables/usePolling'

const router = useRouter()
const servers = ref(null) // null = 尚未載入
const showCreate = ref(false)

const load = () => mcServerApi.list((list) => (servers.value = list), { showError: servers.value === null })
usePolling(load, 4000)

function onCreated(server) {
  router.push({ name: 'mc-overview', params: { id: server.id } })
}
</script>

<template>
  <main class="page">
    <PageHeader title="Minecraft 伺服器" subtitle="建立多個伺服器，各自擁有獨立的主控台、地圖與模組" :back-to="{ name: 'games' }" back-label="遊戲伺服器">
      <template #actions>
        <RouterLink :to="{ name: 'mc-library' }" class="btn ghost sm"><AppIcon name="library" :size="16" /> 資源庫</RouterLink>
        <button class="btn sm" @click="showCreate = true"><AppIcon name="plus" :size="16" /> 新增伺服器</button>
      </template>
    </PageHeader>

    <div v-if="servers && servers.length" class="grid">
      <McServerCard v-for="s in servers" :key="s.id" :server="s" @changed="load" />
    </div>
    <div v-else-if="servers" class="card">
      <EmptyState icon="server" title="還沒有任何伺服器" hint="建立第一個 Minecraft 伺服器：選好版本後，系統會自動下載並安裝。">
        <button class="btn sm" @click="showCreate = true"><AppIcon name="plus" :size="16" /> 新增伺服器</button>
      </EmptyState>
    </div>

    <McCreateServerDialog v-model="showCreate" @created="onCreated" />
  </main>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
</style>
