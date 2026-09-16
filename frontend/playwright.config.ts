import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  workers: 2,
  use: { baseURL: 'http://localhost:4173', ...devices['Desktop Chrome'], channel: 'msedge', trace: 'retain-on-failure' },
  reporter: 'list',
  webServer: { command: 'node node_modules/vite/bin/vite.js preview --host localhost --port 4173', url: 'http://localhost:4173/app/', reuseExistingServer: !process.env.CI },
});
