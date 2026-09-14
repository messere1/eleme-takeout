import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // 联调代理：后端 8080 未配置 CORS，前端 dev 请求统一走本代理转发。
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 上传接口返回的是 /uploads/xxx.png 这种站内路径，由后端静态资源处理。
      // 不代理的话 dev server 会把它当成 SPA 路由、回退成 index.html（text/html），
      // <img> 收到 HTML 就渲染不出来。
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.js'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,vue}'],
      exclude: ['src/test/**'],
    },
  },
})

