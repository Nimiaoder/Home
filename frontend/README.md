# frontend (Vue 3 + Vite)

```
npm install
npm run dev        # http://localhost:5173 ，/api 會代理到 .env.development 的 VITE_PROXY_TARGET
npm run build
```

## 結構
- src/api/http.js      axios 實例（baseURL、逾時、自動帶 token）
- src/api/request.js   **共用請求** `post(url, body, callBack, options)` / `get(...)`
- src/api/auth.js      各模組 API 對應（新模組就新增一個檔案）
- src/stores/auth.js   登入狀態 (Pinia)
- src/router/index.js  路由與登入守衛（預設所有頁面需登入）
- src/utils/toast.js   全域提示訊息
- src/views/           頁面

## 呼叫後端
```js
import { post } from '@/api/request'
post('/api/xxx/list', { page: 1 }, (detail) => { list.value = detail })
// status=1 才會進 callBack；失敗（含伺服器掛掉）自動顯示提示
```

## 功能模組
- 首頁的功能分類寫在 `src/modules/registry.js`；遊戲伺服器模組在 `src/modules/gameserver/`
- 登入後的頁面都掛在 `src/layouts/AppShell.vue` 底下；路由 `meta.roles` 可限制角色
- 主控台使用 SSE（`src/api/stream.js`），開發時 Vite 的 `/api` 代理可直接轉發
- 詳細架構與擴充方式見專案根目錄 `GameServer.md`
