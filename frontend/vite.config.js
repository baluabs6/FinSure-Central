import { defineConfig } from 'vite'; import react from '@vitejs/plugin-react'
export default defineConfig({ plugins: [react()], server: { proxy: {
  '/api/claims': 'http://localhost:8081', '/api/policies': 'http://localhost:8082', '/api/ai': 'http://localhost:8082' } } })
