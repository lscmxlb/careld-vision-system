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
    host: true,
    port: 5172,
    proxy: {
      '/api/v1/auth': {
        target: 'http://127.0.0.1:8281',
        changeOrigin: true,
      },
      '/api/v1/users': {
        target: 'http://127.0.0.1:8282',
        changeOrigin: true,
      },
      '/api/v1/org': {
        target: 'http://127.0.0.1:8282',
        changeOrigin: true,
      },
      '/api/v1/stores': {
        target: 'http://127.0.0.1:8283',
        changeOrigin: true,
      },
      '/api/v1/devices': {
        target: 'http://127.0.0.1:8283',
        changeOrigin: true,
      },
      '/api/v1/device-types': {
        target: 'http://127.0.0.1:8283',
        changeOrigin: true,
      },
      '/api/v1/departments': {
        target: 'http://127.0.0.1:8283',
        changeOrigin: true,
      },
      '/api/v1/children': {
        target: 'http://127.0.0.1:8284',
        changeOrigin: true,
      },
      '/api/v1/schedules': {
        target: 'http://127.0.0.1:8285',
        changeOrigin: true,
      },
      '/api/v1/vision': {
        target: 'http://127.0.0.1:8286',
        changeOrigin: true,
      },
      '/api/v1/sync': {
        target: 'http://127.0.0.1:8287',
        changeOrigin: true,
      },
      '/api/v1/operation-logs': {
        target: 'http://127.0.0.1:8282',
        changeOrigin: true,
      },
      '/api/v1/statistics': {
        target: 'http://127.0.0.1:8282',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://127.0.0.1:8281',
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
