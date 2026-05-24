import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': process.env.VITE_PROXY_API_TARGET ?? 'http://localhost:8080',
      '/central-api': {
        target: process.env.VITE_PROXY_CENTRAL_API_TARGET ?? 'http://localhost:8081',
        rewrite: (path) => path.replace(/^\/central-api/, '/api'),
      },
    },
  },
})
