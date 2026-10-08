<script setup>
import { games } from './games'
import AppIcon from '@/components/ui/AppIcon.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
</script>

<template>
  <main class="page narrow">
    <PageHeader title="遊戲伺服器" subtitle="選擇要管理的遊戲" :back-to="{ name: 'home' }" back-label="首頁" />
    <div class="games">
      <RouterLink v-for="g in games" :key="g.id" :to="g.to" class="game card">
        <span class="ico"><AppIcon :name="g.icon" :size="28" /></span>
        <div>
          <strong>{{ g.name }}</strong>
          <p class="muted small">{{ g.desc }}</p>
        </div>
      </RouterLink>
    </div>
  </main>
</template>

<style scoped>
.games { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.game { display: flex; gap: 16px; align-items: center; padding: 22px; transition: transform .15s, box-shadow .15s, border-color .15s; }
.game:hover { transform: translateY(-2px); border-color: var(--accent); box-shadow: 0 10px 28px rgba(14, 124, 102, .12); }
.ico { width: 56px; height: 56px; border-radius: 16px; display: grid; place-items: center; background: var(--accent-soft); color: var(--accent); flex: none; }
.game strong { font-size: 18px; }
</style>
