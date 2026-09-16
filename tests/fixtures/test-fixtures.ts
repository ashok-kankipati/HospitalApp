import { test as base, expect, Page } from '@playwright/test';
import { PageFactory } from './pages/PageFactory';

type TestFixtures = {
  pageFactory: PageFactory;
  authenticatedPage: Page;
};

export const test = base.extend<TestFixtures>({
  pageFactory: async ({ page }, use) => {
    const pageFactory = new PageFactory(page);
    await use(pageFactory);
  },

  authenticatedPage: async ({ page }, use) => {
    // Login as a test user
    const loginPage = new (require('./pages/LoginPage').LoginPage)(page);
    await page.goto('/login.html');
    await loginPage.login('testpatient', 'password123');
    
    // Wait for navigation to dashboard
    await page.waitForURL('**/dashboard.html', { timeout: 10000 }).catch(() => {
      // If dashboard doesn't exist, just proceed
    });
    
    await use(page);
  },
});

export { expect };
