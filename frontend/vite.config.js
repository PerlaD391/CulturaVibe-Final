import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss()
  ],
  // El Proyecto Final se sirve en la raíz del VPS (no en /t3_act8_eq13/).
  base: '/',
  // Proxy solo para desarrollo local: /api se redirige al backend Spring
  // Boot (http://localhost:8086) para que en local funcione sin CORS.
  // En el VPS, el frontend y la API se sirven desde el mismo origen.
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
    },
  },
})
