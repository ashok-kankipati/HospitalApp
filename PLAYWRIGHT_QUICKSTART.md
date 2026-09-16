# Quick Start Guide - Playwright Tests

## 5-Minute Setup

### 1. Install Dependencies
```bash
npm install
npm run test:install
```

### 2. Configure Test User
Edit `tests/utils/TestData.ts` and update your test credentials:
```typescript
validUser: {
  username: 'your_test_username',
  password: 'your_test_password',
  email: 'test@example.com',
}
```

### 3. Start Your Application
Make sure your Hospital Management System is running:
```bash
mvn spring-boot:run
```
The app should be accessible at `http://localhost:8080`

### 4. Run Tests
```bash
# Run all tests
npm test

# Run tests in UI mode (interactive browser)
npm run test:ui

# Run specific test suite
npm run test:patient

# View report
npm run test:report
```

## Common Commands

| Command | Description |
|---------|-------------|
| `npm test` | Run all tests |
| `npm run test:ui` | Run tests with interactive UI |
| `npm run test:headed` | Run tests with visible browser |
| `npm run test:patient` | Run patient management tests |
| `npm run test:auth` | Run authentication tests |
| `npm run test:performance` | Run performance tests |
| `npm run test:regression` | Run regression tests |
| `npm run test:report` | View HTML test report |
| `npm run test:debug` | Run tests in debug mode |

## What Tests Are Included?

### Authentication (`auth.spec.ts`)
- ✅ Login with valid/invalid credentials
- ✅ User registration
- ✅ Form validation
- ✅ Session management

### Patient Management (`patient.spec.ts`)
- ✅ View patient details
- ✅ Manage medical history
- ✅ Manage allergies
- ✅ Manage chronic conditions
- ✅ View prescriptions
- ✅ Dashboard functionality
- ✅ Patient search

### Performance (`performance.spec.ts`)
- ✅ Page load times
- ✅ Core Web Vitals (FCP, LCP, TTI)
- ✅ Response time to interactions
- ✅ Memory usage
- ✅ Network performance

### Regression (`regression.spec.ts`)
- ✅ UI element visibility
- ✅ Form validation
- ✅ Navigation flow
- ✅ Responsive design
- ✅ Accessibility

## Test Results

Test reports are automatically generated in:
- `playwright-report/` - HTML report (interactive)
- `test-results/` - JSON and XML results

To view the HTML report:
```bash
npm run test:report
```

## Debugging Failed Tests

### Using UI Mode (Recommended)
```bash
npm run test:ui
```
This opens an interactive browser where you can:
- Step through tests
- See what's happening in real-time
- Inspect elements
- Re-run individual tests

### Using Debug Mode
```bash
npm run test:debug
```
Open DevTools in the browser to step through code.

### Check Screenshots/Videos
Failed tests automatically capture:
- Screenshots (in `test-results/`)
- Videos (in `test-results/`)

Look in these folders after test run to see what went wrong.

## Troubleshooting

### "Cannot find module" errors
```bash
# Reinstall dependencies
npm install
npm run test:install
```

### "Connection refused" errors
```
Make sure your Hospital app is running on http://localhost:8080
Check that PostgreSQL is running
```

### Login tests failing
```
1. Update TEST_DATA.validUser in tests/utils/TestData.ts
2. Verify test user exists in the database
3. Check application.properties for correct database config
```

### "Element not found" errors
```bash
# Use UI mode to debug selectors
npm run test:ui

# Or check if DOM structure changed in your HTML files
```

## Running Tests in GitHub Actions

Add to your `.github/workflows/test.yml`:

```yaml
name: Playwright Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: password
          POSTGRES_DB: HospitalApp
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: npm install
      - run: npm run test:install
      - run: mvn spring-boot:run &
      - run: sleep 10  # Wait for app to start
      - run: npm test
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
          retention-days: 30
```

## Next Steps

- Read [PLAYWRIGHT_README.md](./PLAYWRIGHT_README.md) for detailed documentation
- Check page objects in `tests/pages/` to understand selectors
- Review test examples in `tests/*.spec.ts`
- Add new tests following the existing patterns
- Customize performance thresholds in `tests/utils/TestData.ts`

## Tips & Best Practices

✅ **DO:**
- Run tests regularly (after every code change)
- Use UI mode to debug failing tests
- Keep test data current
- Run tests before committing code
- Isolate tests (each test should be independent)

❌ **DON'T:**
- Rely on `page.waitForTimeout()` (use proper waits)
- Share state between tests
- Hard-code test IDs (use page objects)
- Run tests with production data
- Leave failing tests unfixed

## Getting Help

1. Check the test report: `npm run test:report`
2. Look at test videos/screenshots in `test-results/`
3. Run in UI mode: `npm run test:ui`
4. Check browser console for errors
5. Review similar tests as examples

Happy Testing! 🎭
