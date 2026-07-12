import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api/v1/auth': {
        target: 'http://localhost:8281',
        changeOrigin: true,
      },
      '/api/v1/users': {
        target: 'http://localhost:8282',
        changeOrigin: true,
      },
      '/api/v1/stores': {
        target: 'http://localhost:8283',
        changeOrigin: true,
      },
      '/api/v1/departments': {
        target: 'http://localhost:8283',
        changeOrigin: true,
      },
      '/api/v1/children': {
        target: 'http://localhost:8284',
        changeOrigin: true,
      },
      '/api/v1/schedules': {
        target: 'http://localhost:8285',
        changeOrigin: true,
      },
      '/api/v1/vision': {
        target: 'http://localhost:8286',
        changeOrigin: true,
      },
      '/api/v1/sync': {
        target: 'http://localhost:8287',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8281',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
