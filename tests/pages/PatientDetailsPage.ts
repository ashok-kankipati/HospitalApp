import { Page, Locator } from '@playwright/test';

export class PatientDetailsPage {
  readonly page: Page;
  readonly patientName: Locator;
  readonly patientEmail: Locator;
  readonly patientPhone: Locator;
  readonly patientDOB: Locator;
  readonly patientAddress: Locator;
  readonly backButton: Locator;

  // Tab elements
  readonly medicalHistoryTab: Locator;
  readonly allergiesTab: Locator;
  readonly chronicConditionsTab: Locator;
  readonly prescriptionsTab: Locator;

  // Medical History
  readonly addHistoryButton: Locator;
  readonly historyList: Locator;
  readonly historyModal: Locator;
  readonly historyForm: Locator;

  // Allergies
  readonly addAllergyButton: Locator;
  readonly allergyList: Locator;
  readonly allergyModal: Locator;

  // Chronic Conditions
  readonly addConditionButton: Locator;
  readonly conditionList: Locator;
  readonly conditionModal: Locator;

  // Prescriptions
  readonly prescriptionList: Locator;
  readonly printPrescriptionButton: Locator;

  constructor(page: Page) {
    this.page = page;
    
    // Patient Info
    this.patientName = page.locator('#patientName');
    this.patientEmail = page.locator('#patientEmail');
    this.patientPhone = page.locator('#patientPhone');
    this.patientDOB = page.locator('#patientDOB');
    this.patientAddress = page.locator('#patientAddress');
    this.backButton = page.locator('#backBtn');

    // Tabs
    this.medicalHistoryTab = page.locator('[data-tab="medical-history"]');
    this.allergiesTab = page.locator('[data-tab="allergies"]');
    this.chronicConditionsTab = page.locator('[data-tab="chronic-conditions"]');
    this.prescriptionsTab = page.locator('[data-tab="prescriptions"]');

    // Medical History
    this.addHistoryButton = page.locator('#addHistoryBtn');
    this.historyList = page.locator('#historyList');
    this.historyModal = page.locator('#historyModal');
    this.historyForm = page.locator('#historyForm');

    // Allergies
    this.addAllergyButton = page.locator('#addAllergyBtn');
    this.allergyList = page.locator('#allergyList');
    this.allergyModal = page.locator('[data-modal="allergy"]');

    // Chronic Conditions
    this.addConditionButton = page.locator('#addConditionBtn');
    this.conditionList = page.locator('#conditionList');
    this.conditionModal = page.locator('[data-modal="condition"]');

    // Prescriptions
    this.prescriptionList = page.locator('#prescriptionHistoryList');
    this.printPrescriptionButton = page.locator('#printPrescriptionHistoryBtn');
  }

  async goto(patientId: string | number) {
    await this.page.goto(`/patient/${patientId}`);
  }

  async getPatientName() {
    return await this.patientName.textContent();
  }

  async getPatientEmail() {
    return await this.patientEmail.textContent();
  }

  async getPatientPhone() {
    return await this.patientPhone.textContent();
  }

  async clickBackButton() {
    await this.backButton.click();
  }

  // Tab operations
  async clickMedicalHistoryTab() {
    await this.medicalHistoryTab.click();
  }

  async clickAllergiesTab() {
    await this.allergiesTab.click();
  }

  async clickChronicConditionsTab() {
    await this.chronicConditionsTab.click();
  }

  async clickPrescriptionsTab() {
    await this.prescriptionsTab.click();
  }

  // Medical History operations
  async clickAddHistory() {
    await this.addHistoryButton.click();
  }

  async isMedicalHistoryEmpty() {
    const noData = await this.historyList.locator('.no-data').isVisible();
    return noData;
  }

  // Allergies operations
  async clickAddAllergy() {
    await this.addAllergyButton.click();
  }

  async isAllergiesEmpty() {
    const noData = await this.allergyList.locator('.no-data').isVisible();
    return noData;
  }

  // Chronic Conditions operations
  async clickAddCondition() {
    await this.addConditionButton.click();
  }

  async isConditionsEmpty() {
    const noData = await this.conditionList.locator('.no-data').isVisible();
    return noData;
  }

  // Prescriptions operations
  async clickPrintPrescriptions() {
    await this.printPrescriptionButton.click();
  }

  async isPrescriptionsEmpty() {
    const noData = await this.prescriptionList.locator('.no-data').isVisible();
    return noData;
  }
}
