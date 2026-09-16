import { test, expect } from '@playwright/test';
import { PatientDetailsPage } from './pages/PatientDetailsPage';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { TEST_DATA, TIMEOUTS } from './utils/TestData';

test.describe('Patient Management Tests', () => {
    test.describe('Dashboard Page', () => {
    test('should navigate to dashboard', async ({ page }) => {
      const dashboardPage = new DashboardPage(page);
      await dashboardPage.goto();
      
      // Just verify page loads without error
      await expect(page).toHaveTitle(/dashboard|hospital/i);
    });

    test('should logout button be visible when logged in', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(TEST_DATA.validUser.username, TEST_DATA.validUser.password);
      await page.waitForTimeout(3000);
      
      const logoutBtn = page.locator('#logoutBtn, button:has-text("Logout")').first();
      const isVisible = await logoutBtn.isVisible().catch(() => false);
      expect(isVisible).toBeTruthy();
    });
  });

  test.describe('Patient Details Page', () => {
    test('should load patient details page', async ({ page }) => {
      const patientDetailsPage = new PatientDetailsPage(page);
      await page.goto('/patient-details.html?id=1');
      await page.waitForLoadState('networkidle').catch(() => {});
      
      // Just verify page loads
      await expect(page).toHaveTitle(/patient|hospital/i);
    });
  });
});
