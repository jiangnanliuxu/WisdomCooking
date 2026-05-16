import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 管理端开发期通过 Vite 代理转发到网关，避免跨域。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  const cookProxyTarget = env.VITE_COOK_PROXY_TARGET || 'http://localhost:9210'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/auth': proxyTarget,
        '/code': proxyTarget,
        '/system': proxyTarget,
        '/api': cookProxyTarget,
        '/ws': {
          target: proxyTarget,
          ws: true,
          changeOrigin: true,
        },
      },
    },
  }
})
