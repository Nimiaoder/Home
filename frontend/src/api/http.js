import axios from 'axios'
import { storage } from '@/utils/storage'

// axios 實例：統一 baseURL、逾時、自動帶 token
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.request.use((config) => {
  const token = storage.getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default http
