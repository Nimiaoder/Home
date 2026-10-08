<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/ui/PageHeader.vue'
import LibraryVersions from '../components/LibraryVersions.vue'
import LibraryJava from '../components/LibraryJava.vue'
import { logger } from '@/utils/logger'

const log = logger('McLibraryView')
log.debug('*****McLibraryView*****')

// 資源庫：跨伺服器共用的資源（核心版本、Java）。分頁以 ?tab= 記在網址，方便從其他頁面直接連過來。
const route = useRoute()
const router = useRouter()
const tab = computed(() => (route.query.tab === 'java' ? 'java' : 'versions'))
const go = (t) => router.replace({ query: { tab: t } })
</script>

<template>
  <main class="page">
    <PageHeader title="資源庫" subtitle="版本庫與 Java 環境，所有伺服器共用" :back-to="{ name: 'mc-servers' }" back-label="Minecraft 伺服器" />
    <nav class="seg">
      <button :class="{ on: tab === 'versions' }" @click="go('versions')">版本庫</button>
      <button :class="{ on: tab === 'java' }" @click="go('java')">Java 環境</button>
    </nav>
    <LibraryVersions v-if="tab === 'versions'" />
    <LibraryJava v-else />
  </main>
</template>

<style scoped>
.seg { display: inline-flex; padding: 4px; gap: 4px; background: var(--accent-soft); border-radius: 12px; margin-bottom: 20px; }
.seg button { border: 0; background: transparent; padding: 7px 18px; border-radius: 9px; font-weight: 600; color: var(--muted); cursor: pointer; }
.seg button.on { background: var(--surface); color: var(--accent); box-shadow: 0 1px 3px rgba(0, 0, 0, .08); }
</style>
