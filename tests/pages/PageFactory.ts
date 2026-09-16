import { Page } from '@playwright/test';
import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';
import { DashboardPage } from './DashboardPage';
import { PatientDetailsPage } from './PatientDetailsPage';

export class PageFactory {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  getLoginPage(): LoginPage {
    return new LoginPage(this.page);
  }

  getRegisterPage(): RegisterPage {
    return new RegisterPage(this.page);
  }

  getDashboardPage(): DashboardPage {
    return new DashboardPage(this.page);
  }

  getPatientDetailsPage(): PatientDetailsPage {
    return new PatientDetailsPage(this.page);
  }
}

export { LoginPage, RegisterPage, DashboardPage, PatientDetailsPage };
