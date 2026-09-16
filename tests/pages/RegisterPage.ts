import { Page, Locator } from '@playwright/test';

export class RegisterPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly roleSelect: Locator;
  readonly registerButton: Locator;
  readonly errorMessage: Locator;
  readonly successMessage: Locator;
  readonly loginLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('#username');
    this.emailInput = page.locator('#email');
    this.passwordInput = page.locator('#password');
    this.confirmPasswordInput = page.locator('#confirmPassword');
    this.roleSelect = page.locator('#role');
    this.registerButton = page.locator('button.btn-login');
    this.errorMessage = page.locator('#errorMessage');
    this.successMessage = page.locator('#successMessage');
    this.loginLink = page.locator('a[href="login.html"]');
  }

  async goto() {
    await this.page.goto('/register.html');
  }

  async fillUsername(username: string) {
    await this.usernameInput.fill(username);
  }

  async fillEmail(email: string) {
    await this.emailInput.fill(email);
  }

  async fillPassword(password: string) {
    await this.passwordInput.fill(password);
  }

  async fillConfirmPassword(password: string) {
    await this.confirmPasswordInput.fill(password);
  }

  async selectRole(role: string) {
    await this.roleSelect.selectOption(role);
  }

  async clickRegister() {
    await this.registerButton.click();
  }

  async register(username: string, email: string, password: string, role: string) {
    await this.fillUsername(username);
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.fillConfirmPassword(password);
    await this.selectRole(role);
    await this.clickRegister();
  }

  async getErrorMessage() {
    return await this.errorMessage.textContent();
  }

  async getSuccessMessage() {
    return await this.successMessage.textContent();
  }
}
