import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// 用户端 H5 开发同样通过代理访问网关，避免跨域。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [uni()],
    server: {
      host: '0.0.0.0',
      port: 5174,
      proxy: {
        '/api': proxyTarget,
        '/ws': {
          target: proxyTarget,
          changeOrigin: true,
          ws: true,
        },
      },
    },
  }
})
