import { Page, Locator } from '@playwright/test';

export class DashboardPage {
  readonly page: Page;
  readonly logoutButton: Locator;
  readonly patientListContainer: Locator;
  readonly addPatientButton: Locator;
  readonly searchInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.logoutButton = page.locator('button:has-text("Logout")');
    this.patientListContainer = page.locator('[data-testid="patient-list"], .patient-list');
    this.addPatientButton = page.locator('button:has-text("Add Patient"), #addPatientBtn');
    this.searchInput = page.locator('[data-testid="search-input"], .search-input');
  }

  async goto() {
    await this.page.goto('/dashboard.html');
  }

  async isLogoutButtonVisible() {
    return await this.logoutButton.isVisible();
  }

  async clickLogout() {
    await this.logoutButton.click();
  }

  async clickAddPatient() {
    if (await this.addPatientButton.isVisible()) {
      await this.addPatientButton.click();
    }
  }

  async search(query: string) {
    if (await this.searchInput.isVisible()) {
      await this.searchInput.fill(query);
    }
  }

  async getPageTitle() {
    return await this.page.title();
  }
}
