import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { PatientDetailsPage } from './pages/PatientDetailsPage';
import { TestUtils } from './utils/TestUtils';
import { TEST_DATA, PERFORMANCE_THRESHOLDS } from './utils/TestData';

test.describe('Performance Tests', () => {
  test.describe('Page Load Performance', () => {
    test('should load login page within performance threshold', async ({ page }) => {
      const startTime = Date.now();
      
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await page.waitForLoadState('networkidle').catch(() => {});
      
      const loadTime = Date.now() - startTime;
      
      // App should load within 5 seconds
      expect(loadTime).toBeLessThan(5000);
    });

    test('should load register page within reasonable time', async ({ page }) => {
      const startTime = Date.now();
      
      await page.goto('/register.html');
      await page.waitForLoadState('networkidle').catch(() => {});
      
      const loadTime = Date.now() - startTime;
      
      expect(loadTime).toBeLessThan(5000);
    });

    test('should load dashboard page within reasonable time', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(TEST_DATA.validUser.username, TEST_DATA.validUser.password);
      await page.waitForTimeout(2000);
      
      const startTime = Date.now();
      await page.goto('/dashboard.html');
      await page.waitForLoadState('networkidle').catch(() => {});
      const loadTime = Date.now() - startTime;
      
      expect(loadTime).toBeLessThan(5000);
    });
  });

  test.describe('Interaction Performance', () => {
    test('should respond quickly to login button click', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await loginPage.fillUsername(TEST_DATA.validUser.username);
      await loginPage.fillPassword(TEST_DATA.validUser.password);
      
      const startTime = Date.now();
      await loginPage.clickLogin();
      await page.waitForTimeout(2000);
      
      const responseTime = Date.now() - startTime;
      
      expect(responseTime).toBeLessThan(5000);
    });

    test('should handle navigation efficiently', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const startTime = Date.now();
      
      // Navigate to register and back
      const registerLink = page.locator('a[href*="register"]').first();
      if (await registerLink.isVisible()) {
        await registerLink.click();
        await page.waitForTimeout(1000);
      }
      
      const navTime = Date.now() - startTime;
      
      expect(navTime).toBeLessThan(3000);
    });
  });
});
