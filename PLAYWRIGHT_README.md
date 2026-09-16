# Playwright Automation Tests for Hospital Management System

This directory contains comprehensive end-to-end, regression, and performance tests for the Hospital Management System using Playwright with TypeScript.

## Project Structure

```
tests/
├── pages/                    # Page Object Models
│   ├── LoginPage.ts         # Login page objects and actions
│   ├── RegisterPage.ts      # Register page objects and actions
│   ├── DashboardPage.ts     # Dashboard page objects and actions
│   ├── PatientDetailsPage.ts # Patient details page objects
│   └── PageFactory.ts       # Factory for creating page objects
├── fixtures/                 # Test fixtures and setup
│   └── test-fixtures.ts     # Custom fixtures for authenticated tests
├── utils/                    # Utility functions and test data
│   ├── TestUtils.ts         # Helper utilities for tests
│   └── TestData.ts          # Test data and constants
├── auth.spec.ts             # Authentication tests
├── patient.spec.ts          # Patient management tests
├── performance.spec.ts      # Performance tests
├── regression.spec.ts       # Regression tests
├── playwright.config.ts     # Playwright configuration
├── tsconfig.json           # TypeScript configuration
└── package.json            # Dependencies

playwright.html            # HTML test report
```

## Installation

### Prerequisites
- Node.js 16+ and npm
- Hospital Management System running on `http://localhost:8080`
- PostgreSQL database configured and running

### Setup

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Install Playwright browsers:**
   ```bash
   npm run test:install
   ```

3. **Update test credentials:**
   Edit `tests/utils/TestData.ts` and update the `TEST_DATA.validUser` with valid credentials from your application:
   ```typescript
   validUser: {
     username: 'testpatient',  // Update with actual test user
     password: 'password123',   // Update with actual password
     email: 'testpatient@hospital.test',
   },
   ```

## Running Tests

### Run all tests
```bash
npm test
```

### Run tests in UI mode (interactive)
```bash
npm run test:ui
```

### Run tests in debug mode
```bash
npm run test:debug
```

### Run tests with visible browser
```bash
npm run test:headed
```

### Run specific test suites

```bash
# Run authentication tests only
npm run test:auth

# Run patient management tests only
npm run test:patient

# Run performance tests only
npm run test:performance

# Run regression tests only
npm run test:regression
```

### View test report
```bash
npm run test:report
```

## Test Coverage

### Authentication Tests (`auth.spec.ts`)
- Login page display and functionality
- Valid/invalid login attempts
- Required field validation
- Registration functionality
- Password confirmation validation
- Email format validation
- User roles selection
- Session management
- Logout functionality

### Patient Management Tests (`patient.spec.ts`)
- Patient details page display
- Medical history management
  - View medical history
  - Add new medical history
  - Empty state handling
- Allergies management
  - View allergies
  - Add new allergy
- Chronic conditions management
  - View chronic conditions
  - Add new condition
- Prescriptions management
  - View prescription history
  - Download/print prescriptions
- Dashboard functionality
- Patient search

### Performance Tests (`performance.spec.ts`)
- Page load time performance
- Core Web Vitals (FCP, LCP, TTI)
- Interaction performance
  - Login response time
  - Tab switching efficiency
  - Search input response
- Memory and resource usage
- Network performance
- Asset caching

### Regression Tests (`regression.spec.ts`)
- UI element visibility
- Form validation
- API error handling
- Navigation flow
- Browser back/forward buttons
- Responsive design (mobile, tablet, desktop)
- Accessibility requirements
- Form labels and heading hierarchy

## Configuration

### Playwright Configuration (`playwright.config.ts`)

Key configurations:
- **Browsers**: Tests run against Chromium, Firefox, and WebKit
- **Base URL**: `http://localhost:8080`
- **Timeout**: 30 seconds per test
- **Retries**: 2 retries in CI environment
- **Screenshots**: Captured on failure
- **Videos**: Retained on test failure
- **Trace**: Captured on first retry

### Modifying Configuration

Edit `playwright.config.ts` to:
- Change the base URL if your app runs on a different port
- Adjust timeouts
- Add/remove browsers
- Configure headless mode
- Set up custom project configurations

## Page Object Model

The tests use the Page Object Model (POM) pattern for better maintainability:

```typescript
// Example: Using LoginPage
const loginPage = new LoginPage(page);
await loginPage.goto();
await loginPage.login('username', 'password');
```

### Available Page Objects
- **LoginPage**: Login form and related actions
- **RegisterPage**: Registration form and validation
- **DashboardPage**: Dashboard navigation and patient list
- **PatientDetailsPage**: Patient information and medical records tabs
- **PageFactory**: Factory for creating page objects

## Test Data

Test data is centralized in `tests/utils/TestData.ts`:

```typescript
export const TEST_DATA = {
  validUser: { username, password, email },
  patientRegisterData: { username, email, password, role },
  patientInfo: { name, email, phone, dob, address },
  // ... more test data
};
```

## Utilities

### TestUtils
Helper methods for:
- Generating unique IDs and emails for test data
- Waiting for network idle
- Checking element existence
- Measuring page load times
- Capturing performance metrics
- Collecting console messages

### Usage example:
```typescript
const uniqueEmail = TestUtils.generateUniqueEmail();
const metrics = await TestUtils.getPerformanceMetrics(page);
```

## CI/CD Integration

The tests are configured to run in CI environments:

```bash
# In CI pipeline
npm run test
```

The configuration automatically:
- Runs tests serially in CI (parallelism disabled)
- Retries failed tests twice
- Generates HTML, JSON, and JUnit reports
- Captures screenshots and videos on failure

## Troubleshooting

### Tests Failing with Connection Errors
- Ensure the Spring Boot application is running on port 8080
- Check that PostgreSQL is running and accessible
- Verify database credentials in `application.properties`

### Login Tests Failing
- Update test credentials in `TestData.ts`
- Ensure test user exists in the database
- Check that authentication is working in the UI

### Performance Tests Failing
- Reduce the `PERFORMANCE_THRESHOLDS` if your system is slower
- Edit `tests/utils/TestData.ts` to adjust thresholds
- Check network conditions and system resources

### Element Not Found Errors
- Verify selectors match your current HTML structure
- Update selectors in page objects if DOM changed
- Use UI mode (`npm run test:ui`) to debug selectors

## Best Practices

1. **Use Page Objects**: Don't query selectors directly in tests, use page objects
2. **Meaningful Test Names**: Test names should describe what is being tested
3. **Test Isolation**: Each test should be independent and not rely on others
4. **Realistic Data**: Use test data that mimics real scenarios
5. **Wait Strategies**: Use `waitForLoadState` or explicit waits instead of `page.waitForTimeout`
6. **Error Handling**: Handle expected errors gracefully in tests
7. **Performance Awareness**: Monitor test execution time and optimize slow tests

## Adding New Tests

1. Create a new test file in `tests/` directory with `.spec.ts` extension
2. Use existing page objects or create new ones
3. Follow the test structure of existing tests
4. Import `TestData` and `TestUtils` as needed
5. Update `playwright.config.ts` if adding new test categories

Example:
```typescript
import { test, expect } from '@playwright/test';
import { PatientDetailsPage } from '../pages/PatientDetailsPage';

test.describe('New Feature Tests', () => {
  test('should do something', async ({ page }) => {
    const patientDetailsPage = new PatientDetailsPage(page);
    // Test implementation
  });
});
```

## Reports and Artifacts

After running tests, artifacts are available in:
- `playwright-report/`: HTML test report
- `test-results/`: Test result JSON and JUnit XML files
- Individual test folders: Screenshots and videos

Open the HTML report:
```bash
npm run test:report
```

## Contact & Support

For issues or questions about the tests, please check:
1. Test failures in the HTML report
2. Screenshots/videos in test results folders
3. Console output for detailed error messages
