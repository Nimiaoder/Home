<script setup>
import { computed } from 'vue'

// 簡單的線條圖示集。新增圖示：在 PATHS 加一筆（24x24 viewBox，stroke 風格）。
const PATHS = {
  gamepad: 'M6 12h4M8 10v4M15 13h.01M18 11h.01M17.3 5H6.7a4 4 0 0 0-3.9 3.1l-1.2 5.5A3 3 0 0 0 4.5 17c1 0 1.9-.5 2.4-1.3L8 14h8l1.1 1.7c.5.8 1.4 1.3 2.4 1.3a3 3 0 0 0 2.9-3.4l-1.2-5.5A4 4 0 0 0 17.3 5Z',
  server: 'M3 4h18v6H3zM3 14h18v6H3zM7 7h.01M7 17h.01',
  cube: 'M12 2 3 7v10l9 5 9-5V7zM3 7l9 5 9-5M12 12v10',
  play: 'M7 4v16l13-8z',
  stop: 'M6 6h12v12H6z',
  restart: 'M20 11a8 8 0 1 0-2.3 5.7M20 4v7h-7',
  power: 'M12 3v9M6.4 6.4a8 8 0 1 0 11.2 0',
  terminal: 'M4 5h16v14H4zM7 10l3 2-3 2M12 15h5',
  globe: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM3 12h18M12 3c3 3.5 3 14.5 0 18M12 3c-3 3.5-3 14.5 0 18',
  puzzle: 'M10 4a2 2 0 1 1 4 0v2h4v4h-2a2 2 0 1 0 0 4h2v4h-4v-2a2 2 0 1 0-4 0v2H6v-4h2a2 2 0 1 0 0-4H6V6h4z',
  tag: 'M3 12V3h9l9 9-9 9zM7.5 7.5h.01',
  sliders: 'M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0M14 4v4M8 10v4M16 16v4',
  folder: 'M3 6h6l2 2h10v11H3z',
  upload: 'M12 16V4M7 9l5-5 5 5M4 20h16',
  download: 'M12 4v12M7 11l5 5 5-5M4 20h16',
  trash: 'M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6',
  plus: 'M12 5v14M5 12h14',
  back: 'M15 5l-7 7 7 7',
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4',
  check: 'M5 12l5 5 9-10',
  x: 'M6 6l12 12M18 6 6 18',
  alert: 'M12 3 2 20h20zM12 10v5M12 18h.01',
  users: 'M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM2 20c0-3.5 3-6 7-6s7 2.5 7 6M17 4.5a3.5 3.5 0 0 1 0 6.5M18 14c2.5.5 4 2.5 4 6',
  memory: 'M7 7h10v10H7zM9 3v4M15 3v4M9 17v4M15 17v4M3 9h4M3 15h4M17 9h4M17 15h4',
  clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 7v5l3 2',
  package: 'M3 8l9-5 9 5v8l-9 5-9-5zM3 8l9 5 9-5M12 13v8',
  coffee: 'M4 8h12v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5zM16 9h2a2 2 0 0 1 0 5h-2M7 3v2M11 3v2',
  logout: 'M10 4H5v16h5M14 8l4 4-4 4M18 12H9',
  archive: 'M3 5h18v4H3zM5 9v10h14V9M10 13h4',
  library: 'M4 4v16M9 4v16M14 6l4 14M20 5l-3 1'
}

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 }
})
const d = computed(() => PATHS[props.name] || '')
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="icon">
    <path :d="d" />
  </svg>
</template>

<style scoped>
.icon { flex: none; }
</style>
