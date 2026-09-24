import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

const apiProxy = (target: string) => ({ target, changeOrigin: true })

export default defineConfig({
  plugins: [uni()],
  server: {
    host: true,
    port: Number(process.env.PORT) || 5177,
    proxy: {
      '/api/v1/auth': apiProxy('http://127.0.0.1:8281'),
      '/api/v1/users': apiProxy('http://127.0.0.1:8282'),
      '/api/v1/medical-staff': apiProxy('http://127.0.0.1:8282'),
      '/api/v1/operation-logs': apiProxy('http://127.0.0.1:8282'),
      '/api/v1/statistics': apiProxy('http://127.0.0.1:8282'),
      '/api/v1/stores': apiProxy('http://127.0.0.1:8283'),
      '/api/v1/devices': apiProxy('http://127.0.0.1:8283'),
      '/api/v1/departments': apiProxy('http://127.0.0.1:8283'),
      '/api/v1/children': apiProxy('http://127.0.0.1:8284'),
      '/api/v1/schedule-rules': apiProxy('http://127.0.0.1:8285'),
      '/api/v1/appointment-config': apiProxy('http://127.0.0.1:8285'),
      '/api/v1/schedules': apiProxy('http://127.0.0.1:8285'),
      '/api/v1/vision': apiProxy('http://127.0.0.1:8286'),
      '/api/v1/care-records': apiProxy('http://127.0.0.1:8286'),
      '/api/v1/sync': apiProxy('http://127.0.0.1:8287'),
      '/api/v1/notify': apiProxy('http://127.0.0.1:8288'),
      '/api': apiProxy('http://127.0.0.1:8281'),
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
