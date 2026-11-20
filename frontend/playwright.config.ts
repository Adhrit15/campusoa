import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end smoke test: a real browser against the production build (vite preview), which proxies
 * /api to a REAL backend on :8080 (started separately with the dev profile, so demo data exists).
 * CI starts Postgres + backend first; locally run the backend yourself (see README).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'mobile-chrome', use: { ...devices['Pixel 7'] } },   // most students are on phones
  ],
  webServer: {
    command: 'npm run build && npm run preview',
    url: 'http://localhost:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
