/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE?: string
  readonly VITE_ADMIN_LABELS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
