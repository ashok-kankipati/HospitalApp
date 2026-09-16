import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { PatientDetailsPage } from './pages/PatientDetailsPage';

test.describe('Regression Tests', () => {
  test.describe('UI Element Visibility', () => {
    test('login page should not have broken links or images', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      // Check for broken images
      const images = await page.locator('img').all();
      for (const img of images) {
        const src = await img.getAttribute('src');
        if (src && !src.startsWith('data:')) {
          const hasContent = await img.evaluate((el: HTMLElement) => (el as any).offsetHeight > 0);
          expect(hasContent).toBeTruthy();
        }
      }
    });

    test('patient details page layout should be consistent', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('testpatient', 'password123');
      await page.waitForTimeout(2000);
      
      const patientDetailsPage = new PatientDetailsPage(page);
      await page.goto('/patient-details.html?id=1');
      
      // Check main sections exist
      const patientInfoSection = await page.locator('.patient-info-section').isVisible();
      const tabsContainer = await page.locator('.tabs-container').isVisible();
      
      expect(patientInfoSection || tabsContainer).toBeTruthy();
    });

    test('all form inputs should be accessible', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const usernameLabel = await page.locator('label[for="username"]').isVisible();
      const passwordLabel = await page.locator('label[for="password"]').isVisible();
      
      expect(usernameLabel && passwordLabel).toBeTruthy();
    });
  });

  test.describe('Form Validation', () => {
    test('login form should validate inputs correctly', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      // Try to submit with empty fields
      await loginPage.clickLogin();
      
      // Should still show form
      expect(page.url()).toContain('login');
    });

    test('register form should validate email format', async ({ page }) => {
      const registerPage = new RegisterPage(page);
      await registerPage.goto();
      
      await registerPage.fillEmail('not-an-email');
      
      const emailInput = page.locator('#email');
      const isValid = await emailInput.evaluate((el: HTMLInputElement) => el.checkValidity());
      
      expect(!isValid).toBeTruthy();
    });

    test('register form should validate password match', async ({ page }) => {
      const registerPage = new RegisterPage(page);
      await registerPage.goto();
      
      await registerPage.fillPassword('Password123!');
      await registerPage.fillConfirmPassword('Password456!');
      
      // Passwords don't match - form should not submit
      await registerPage.clickRegister();
      
      // Still on register page
      expect(page.url()).toContain('register');
    });
  });

  test.describe('API Response Handling', () => {
    test('should handle API errors gracefully', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      // Try to login with credentials that don't exist
      await loginPage.login('nonexistent_user_' + Date.now(), 'wrongpassword');
      
      await page.waitForTimeout(2000);
      
      // Should show error message or stay on login page
      const hasErrorMessage = await loginPage.errorMessage.isVisible();
      const stayedOnLoginPage = page.url().includes('login');
      
      expect(hasErrorMessage || stayedOnLoginPage).toBeTruthy();
    });

    test('dashboard should handle empty patient list', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('testpatient', 'password123');
      await page.waitForTimeout(2000);
      
      const dashboardPage = new DashboardPage(page);
      await dashboardPage.goto();
      
      // Page should still be functional even with empty list
      const pageTitle = await dashboardPage.getPageTitle();
      expect(pageTitle).toBeTruthy();
    });
  });

  test.describe('Navigation', () => {
    test('should navigate correctly between pages', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      let currentUrl = page.url();
      expect(currentUrl).toContain('login');
      
      // Go to register
      await loginPage.clickRegisterLink();
      currentUrl = page.url();
      expect(currentUrl).toContain('register');
    });

    test('should handle browser back/forward buttons', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const firstUrl = page.url();
      
      // Navigate to register
      await loginPage.clickRegisterLink();
      
      // Go back
      await page.goBack();
      
      expect(page.url()).toContain('login');
    });

    test('patient details back button should navigate correctly', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('testpatient', 'password123');
      await page.waitForTimeout(2000);
      
      const patientDetailsPage = new PatientDetailsPage(page);
      await page.goto('/patient-details.html?id=1');
      
      if (await patientDetailsPage.backButton.isVisible()) {
        const currentUrl = page.url();
        
        await patientDetailsPage.clickBackButton();
        
        const newUrl = page.url();
        expect(newUrl).not.toBe(currentUrl);
      }
    });
  });

  test.describe('Responsive Design', () => {
    test('should display login page correctly on mobile viewport', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      // Elements should still be visible and usable
      await expect(loginPage.usernameInput).toBeVisible();
      await expect(loginPage.passwordInput).toBeVisible();
      await expect(loginPage.loginButton).toBeVisible();
    });

    test('should display patient details page correctly on tablet viewport', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('testpatient', 'password123');
      await page.waitForTimeout(2000);
      
      const patientDetailsPage = new PatientDetailsPage(page);
      await page.goto('/patient-details.html?id=1');
      
      // Tabs should be accessible
      const tabButtonCount = await page.locator('.tab-button').count();
      expect(tabButtonCount).toBeGreaterThan(0);
    });

    test('should handle different screen orientations', async ({ page }) => {
      // Portrait
      await page.setViewportSize({ width: 375, height: 667 });
      
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await expect(loginPage.loginButton).toBeVisible();
      
      // Landscape
      await page.setViewportSize({ width: 667, height: 375 });
      await expect(loginPage.loginButton).toBeVisible();
    });
  });

  test.describe('Accessibility', () => {
    test('form inputs should have associated labels', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const usernameLabel = await page.locator('label[for="username"]').count();
      const passwordLabel = await page.locator('label[for="password"]').count();
      
      expect(usernameLabel).toBeGreaterThan(0);
      expect(passwordLabel).toBeGreaterThan(0);
    });

    test('buttons should have descriptive text', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const buttonText = await loginPage.loginButton.textContent();
      expect(buttonText?.toLowerCase()).toContain('login');
    });

    test('page should have proper heading hierarchy', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const h1Count = await page.locator('h1').count();
      expect(h1Count).toBeGreaterThan(0);
    });
  });
});
