// Disposable integration server: keep the user's 5173/8080 services untouched.
import { defineConfig } from 'vite'
import base from '../vite.config.js'

export default defineConfig({
  ...base,
  server: {
    ...base.server,
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:18081',
        changeOrigin: true,
      },
    },
  },
})
