import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [vue()],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: {
      port: 5173,
      host: true,          // 監聽 0.0.0.0：可用 localhost / 內網 IP / 對外 IP 存取
      allowedHosts: true,
      // 開發時把 /api 轉發到後端，避免 CORS；後端網址在 .env.development 設定
      proxy: { '/api': { target: env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } }
    }
  }
})
