import { defineConfig } from 'vite'
import pluginReact from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [pluginReact()],
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    strictPort: true,
  },
  preview: {
    port: 4173,
  }
})