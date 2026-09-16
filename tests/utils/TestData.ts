/**
 * Test data constants for patient management tests
 */
export const TEST_DATA = {
  validUser: {
    username: 'dr_smith',
    password: '123456',
    email: 'ashokkanki756951@gmail.com',
  },

  invalidUser: {
    username: 'invaliduser',
    password: 'wrongpassword',
  },

  patientRegisterData: {
    username: 'newpatient',
    email: 'newpatient@hospital.test',
    password: 'SecurePass123!',
    role: 'Patient',
  },

  doctorRegisterData: {
    username: 'testdoctor',
    email: 'doctor@hospital.test',
    password: 'SecurePass123!',
    role: 'Doctor',
  },

  staffRoles: [
    'Admin',
    'Doctor',
    'Nurse',
    'Patient',
    'Receptionist',
    'Lab Technician',
    'Pharmacist',
    'Radiologist',
    'Surgeon',
  ],

  patientInfo: {
    name: 'John Doe',
    email: 'john.doe@example.com',
    phone: '555-123-4567',
    dob: '1990-01-15',
    address: '123 Main St, City, State 12345',
  },

  medicalHistory: {
    condition: 'Hypertension',
    diagnosis: 'High blood pressure',
    dateOfDiagnosis: '2020-06-01',
    treatment: 'Medication ongoing',
  },

  allergy: {
    allergen: 'Penicillin',
    severity: 'Severe',
    reaction: 'Anaphylaxis',
  },

  chronicCondition: {
    condition: 'Type 2 Diabetes',
    startDate: '2018-03-10',
    status: 'Active',
    notes: 'Well controlled with medication',
  },
};

/**
 * API endpoints
 */
export const API_ENDPOINTS = {
  login: '/api/auth/login',
  register: '/api/auth/register',
  patients: '/api/patients',
  patientDetails: (id: number | string) => `/api/patients/${id}`,
  patientSearch: (query: string) => `/api/patients/search?q=${query}`,
  medicalHistory: (patientId: number | string) => `/api/patients/${patientId}/medical-history`,
  allergies: (patientId: number | string) => `/api/patients/${patientId}/allergies`,
  chronicConditions: (patientId: number | string) => `/api/patients/${patientId}/chronic-conditions`,
  prescriptions: (patientId: number | string) => `/api/patients/${patientId}/prescriptions`,
};

/**
 * Page URLs
 */
export const PAGE_URLS = {
  login: '/login.html',
  register: '/register.html',
  dashboard: '/dashboard.html',
  patientDetails: (patientId: number | string) => `/patient-details.html?id=${patientId}`,
};

/**
 * Test timeouts (in milliseconds)
 */
export const TIMEOUTS = {
  short: 5000,      // 5 seconds
  medium: 10000,    // 10 seconds
  long: 30000,      // 30 seconds
  veryLong: 60000,  // 60 seconds
};

/**
 * Performance thresholds (in milliseconds)
 */
export const PERFORMANCE_THRESHOLDS = {
  pageLoadTime: 3000,          // Page should load within 3 seconds
  firstContentfulPaint: 2000,  // FCP within 2 seconds
  timeToInteractive: 3500,     // TTI within 3.5 seconds
  apiResponseTime: 1000,       // API responses within 1 second
};
