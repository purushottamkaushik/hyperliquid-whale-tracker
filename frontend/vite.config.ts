import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    // Emit straight into the Spring Boot static resources folder so the jar serves the SPA.
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
    // Allows the dev server to be reached through an ngrok tunnel (free-tier subdomains are
    // random per session, so the whole ngrok-free.dev domain is allow-listed, not just one host).
    allowedHosts: ['.ngrok-free.dev'],
  },
})
