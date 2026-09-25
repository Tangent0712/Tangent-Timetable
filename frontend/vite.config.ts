import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // antd 作为独立 vendor chunk（约 1.1MB / gzip 360KB），单独缓存
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        // 拆分体积较大的第三方库，改善首屏加载与缓存命中
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          antd: ['antd', '@ant-design/icons'],
        },
      },
    },
  },
})
