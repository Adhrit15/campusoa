/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies API and OAuth paths to Spring Boot, so the browser sees ONE origin
// (http://localhost:5173), exactly like production behind Vercel's rewrites. That's why we
// need no CORS and the session cookie is first-party.
const target = process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080';
const proxied = { target, changeOrigin: false, xfwd: true };

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': proxied,
      '/oauth2': proxied,
      '/login/oauth2': proxied,
      '/actuator': proxied,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: { reporter: ['text', 'html'], include: ['src/**'], exclude: ['src/test/**', 'src/main.tsx'] },
  },
});
