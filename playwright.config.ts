import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  
  reporter: [
    ['html'],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
    ['list']
  ],

  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',  // Changed from 'retain-on-failure' to 'off' - requires ffmpeg
    actionTimeout: 10000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: process.env.CI 
    ? {
        command: 'mvn spring-boot:run',
        url: 'http://localhost:8080',
        reuseExistingServer: false,
        timeout: 180000,
      }
    : undefined,

  globalTimeout: 30 * 60 * 1000, // 30 minutes
  timeout: 30000, // 30 seconds per test
});
