/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ADMIN_API_BASE: string
  readonly VITE_GATEWAY_API_BASE: string
  readonly VITE_COOK_API_BASE: string
  readonly VITE_PROXY_TARGET: string
  readonly VITE_COOK_PROXY_TARGET: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
