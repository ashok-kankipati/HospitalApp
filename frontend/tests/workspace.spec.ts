import { test, expect, type Page } from '@playwright/test';

const user = { username: 'Bhargavi', email: 'staff@example.test', role: 'Doctor', success: true };
const patient = { id: 1, name: 'Ananya Rao', email: 'ananya@example.test', phone: '9000000001', address: 'Hyderabad', dateOfBirth: '1994-03-12', medicalHistory: '', isActive: true };
async function mockApi(page: Page, authenticated = true) {
  const patients = [patient];
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const method = route.request().method();
    if (path === '/api/auth/session') return route.fulfill({ status: authenticated ? 200 : 401, json: authenticated ? user : {} });
    if (path === '/api/auth/login') return route.fulfill({ json: user });
    if (path === '/api/auth/logout') return route.fulfill({ status: 204 });
    if (path === '/api/patients' && method === 'POST') { patients.push({ ...route.request().postDataJSON(), id: 2 }); return route.fulfill({ json: patients[1] }); }
    if (path === '/api/patients') return route.fulfill({ json: patients });
    if (path === '/api/patients/1') return route.fulfill({ json: patient });
    if (path === '/api/staff') return route.fulfill({ json: [{ id: 1, name: 'Dr. Meera Shah', role: 'Doctor' }] });
    if (path === '/api/beds/summary') return route.fulfill({ json: { total: 12, available: 8, occupied: 4, icu: 2, general: 6, private: 4 } });
    if (path === '/api/appointments') return route.fulfill({ json: [{ id: 1, patientId: 1, staffId: 1, appointmentDate: new Date().toLocaleDateString('en-CA'), appointmentTime: '09:30', status: 'CONFIRMED', reason: 'Follow-up consultation' }] });
    return route.fulfill({ json: [] });
  });
}

test('overview uses API data and opens the existing scheduling workflow', async ({ page }) => {
  const errors: string[] = []; page.on('pageerror', e => errors.push(e.message));
  await mockApi(page); await page.goto('/app/');
  await expect(page.getByRole('heading', { name: /Good .*Bhargavi/ })).toBeVisible();
  await expect(page.locator('.cf-metric').filter({ hasText: 'Available beds' }).locator('strong')).toHaveText('8');
  await page.getByRole('button', { name: 'Schedule visit', exact: true }).click();
  await expect(page.locator('#scheduleAppointmentModal')).toBeVisible();
  expect(errors).toEqual([]);
});

test('patient directory searches, creates a record, and refreshes data', async ({ page }) => {
  await mockApi(page); await page.goto('/app/#/patients');
  await page.getByRole('textbox', { name: 'Search patients' }).fill('not found');
  await expect(page.getByRole('heading', { name: 'No matching patients' })).toBeVisible();
  await page.getByRole('textbox', { name: 'Search patients' }).fill('');
  await page.getByRole('button', { name: 'Add patient', exact: true }).first().click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Full name').fill('Test Patient');
  await dialog.getByLabel('Email address').fill('test@example.test');
  await dialog.getByLabel('Phone number').fill('9000000002');
  await dialog.getByLabel('Date of birth').fill('2000-01-01');
  await dialog.getByRole('button', { name: 'Add patient', exact: true }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page.locator('.cf-directory-table').getByText('Test Patient', { exact: true })).toBeVisible();
});

test('clinical navigation preserves all existing sections', async ({ page }) => {
  const errors: string[] = []; page.on('pageerror', e => errors.push(e.message));
  await mockApi(page);
  await page.route('**/api/auth/session', route => route.fulfill({ json: { ...user, role: 'Admin' } }));
  await page.goto('/app/');
  for (const [label, id] of [['Appointments', 'appointments'], ['Pharmacy', 'pharmacy'], ['Laboratory', 'laboratory'], ['Billing', 'billing'], ['Beds & admissions', 'beds'], ['Care team', 'staff'], ['Settings', 'settings']]) {
    await page.locator('.cf-sidebar').getByRole('link', { name: label, exact: true }).click();
    await expect(page.locator(`#${id}Section`)).toBeVisible();
  }
  expect(errors).toEqual([]);
});

test('sign-in displays backend errors and hands off to Duo without storing a user', async ({ page }) => {
  await mockApi(page, false); await page.goto('/app/#/login');
  await page.route('**/api/auth/login', route => route.fulfill({ status: 401, json: { message: 'Invalid username or password' } }));
  await page.getByLabel('Username', { exact: true }).fill('Bhargavi');
  await page.getByLabel('Password', { exact: true }).fill('incorrect');
  await page.getByRole('button', { name: 'Sign in to workspace' }).click();
  await expect(page.getByRole('alert')).toContainText('Invalid username or password');
  await page.route('**/api/auth/login', route => route.fulfill({ json: { success: false, mfaRequired: true, redirectUrl: 'http://localhost:4173/duo-test' } }));
  await page.route('**/duo-test', route => route.fulfill({ contentType: 'text/html', body: '<h1>Duo test handoff</h1>' }));
  await page.getByRole('button', { name: 'Sign in to workspace' }).click();
  await expect(page).toHaveURL(/duo-test/);
  expect(await page.evaluate(() => localStorage.getItem('user'))).toBeNull();
});

test('Duo return uses the server session; failure offers a new login', async ({ page }) => {
  await mockApi(page); await page.goto('/app/#/login?duo=complete');
  await expect(page.getByRole('heading', { name: /Good .*Bhargavi/ })).toBeVisible();
  // A new Duo callback is a full navigation, not a hash change in the active workspace.
  await page.goto('about:blank');
  await page.route('**/api/auth/session', route => route.fulfill({ status: 401, json: {} }));
  await page.goto('/app/#/login?duo=failed');
  await expect(page.getByRole('alert')).toContainText('Duo verification failed');
});

test('mobile navigation, keyboard search, and dialog dismissal work', async ({ page }) => {
  await mockApi(page); await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/app/');
  await page.getByRole('button', { name: 'Open navigation' }).click();
  await page.locator('.cf-sidebar').getByRole('link', { name: 'Patients', exact: true }).click();
  await expect(page.locator('.cf-sidebar')).not.toHaveClass(/cf-sidebar-open/);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.keyboard.press('Control+k');
  await page.getByRole('textbox', { name: 'Search workspace' }).fill('pharmacy');
  await page.getByRole('dialog').getByRole('button', { name: /Pharmacy/ }).click();
  await expect(page).toHaveURL(/pharmacy/);
  await page.keyboard.press('Control+k'); await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).not.toBeVisible();
});

test('screenshots of desktop and mobile use synthetic test data', async ({ page }) => {
  await mockApi(page); await page.setViewportSize({ width: 1440, height: 1040 }); await page.goto('/app/');
  await expect(page.locator('.cf-metric').first().locator('strong')).toHaveText('1');
  await page.screenshot({ path: 'artifacts/overview-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: 'artifacts/overview-mobile.png', fullPage: true, animations: 'disabled' });
  await page.route('**/api/auth/session', route => route.fulfill({ status: 401, json: {} }));
  await page.reload(); await expect(page.getByRole('button', { name: 'Sign in to workspace' })).toBeVisible();
  await page.setViewportSize({ width: 1440, height: 960 });
  await page.screenshot({ path: 'artifacts/login-desktop.png', fullPage: true });
});


test('non-admin cannot open account management', async ({ page }) => {
  await mockApi(page); await page.goto('/app/#/account');
  await expect(page).toHaveURL(/overview/);
  await expect(page.getByRole('button', { name: 'Create account', exact: true })).toHaveCount(0);
  await expect(page.locator('.cf-sidebar').getByRole('link', { name: 'Staff accounts' })).toHaveCount(0);
});

test('admin creates accounts, changes roles, and confirms deletion', async ({ page }) => {
  await mockApi(page);
  await page.route('**/api/auth/session', route => route.fulfill({ json: { ...user, role: 'Admin' } }));
  let accounts = [{ id: 1, ...user, role: 'Admin', active: true }];
  await page.route('**/api/admin/accounts**', route => {
    const method = route.request().method();
    if (method === 'POST') accounts.push({ ...route.request().postDataJSON(), id: 2, active: true });
    if (method === 'PUT') accounts[1].role = route.request().postDataJSON().role;
    if (method === 'DELETE') { accounts = accounts.slice(0, 1); return route.fulfill({ status: 204 }); }
    return route.fulfill({ json: method === 'GET' ? accounts : accounts[accounts.length - 1] });
  });
  await page.goto('/app/#/account');
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Username', { exact: true }).fill('new_doctor');
  await dialog.getByLabel('Email', { exact: true }).fill('new@example.test');
  await dialog.getByLabel('Password', { exact: true }).fill('Strong-test-password');
  await dialog.getByLabel('Confirm password').fill('Strong-test-password');
  await dialog.getByRole('button', { name: 'Save account' }).click();
  await expect(page.getByRole('cell', { name: 'new_doctor', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Change role for new_doctor' }).click();
  await dialog.getByLabel('Role', { exact: true }).selectOption('Nurse');
  await dialog.getByRole('button', { name: 'Save account' }).click();
  await expect(page.getByRole('cell', { name: 'Nurse', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Delete new_doctor', exact: true }).click();
  await dialog.getByRole('button', { name: 'Delete account', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'new_doctor', exact: true })).toHaveCount(0);
});

test('React and clinical status labels have matching dimensions and simple dots', async ({ page }) => {
  await mockApi(page); await page.goto('/app/');
  const overview = page.locator('.cf-badge').first(); await expect(overview).toBeVisible();
  const size = await overview.boundingBox();
  expect(size!.width).toBeLessThan(90); expect(size?.height).toBe(20);
  expect(await overview.evaluate(el => getComputedStyle(el).backgroundColor)).toBe('rgba(0, 0, 0, 0)');
  await page.locator('.cf-sidebar').getByRole('link', { name: 'Appointments', exact: true }).click();
  const status = page.locator('#appointmentsSection .cf-status-pill').first(); await expect(status).toBeVisible();
  const clinicalSize = await status.boundingBox();
  expect(clinicalSize?.width).toBe(size?.width); expect(clinicalSize?.height).toBe(size?.height);
  expect(await status.evaluate(el => getComputedStyle(el, '::before').maskImage)).toBe('none');
  await page.screenshot({ path: 'artifacts/status-and-actions.png', fullPage: true });
});


test('account workspace does not preload clinical data', async ({ page }) => {
  const paths: string[] = [];
  page.on('request', request => { if (request.url().includes('/api/')) paths.push(new URL(request.url()).pathname); });
  await mockApi(page);
  await page.route('**/api/auth/session', route => route.fulfill({ json: { ...user, role: 'Admin' } }));
  await page.goto('/app/#/account');
  await expect(page.getByRole('button', { name: 'Create account', exact: true })).toBeVisible();
  expect(paths).not.toContain('/api/patients');
  expect(paths).not.toContain('/api/appointments');
  expect(paths).not.toContain('/api/staff');
});

test('legacy screens strip executable patient markup and keep row actions usable', async ({ page }) => {
  await mockApi(page);
  await page.route('**/api/appointments/1', route => route.fulfill({ json: { id: 1, patientId: 1, staffId: 1, status: 'CONFIRMED', appointmentDate: '2026-09-16', appointmentTime: '09:30' } }));
  await page.route('**/api/patients*', route => route.fulfill({ json: [{ ...patient, name: '<img src=x onerror="window.attacked=true">Test patient' }] }));
  await page.goto('/app/#/appointments');
  const row = page.locator('#appointmentsSection tbody tr').first();
  await expect(row).toContainText('Test patient');
  expect(await page.evaluate(() => (window as unknown as { attacked?: boolean }).attacked)).toBeUndefined();
  await expect(row.locator('[onerror], [onclick]')).toHaveCount(0);
  await row.getByRole('button', { name: 'View appointment', exact: true }).click();
  await expect(page.locator('#viewAppointmentModal')).toBeVisible();
});


test('pharmacist can view patients and dispense but cannot register or schedule', async ({ page }) => {
  await mockApi(page);
  await page.route('**/api/auth/session', route => route.fulfill({ json: { ...user, role: 'Pharmacist' } }));
  await page.goto('/app/');
  await expect(page.getByRole('button', { name: 'Add patient', exact: true }).first()).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Schedule visit', exact: true })).toBeDisabled();
  await page.locator('.cf-sidebar').getByRole('link', { name: 'Patients', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Add patient', exact: true }).first()).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Edit Ananya Rao', exact: true })).toBeDisabled();
  await expect(page.getByRole('link', { name: 'View Ananya Rao', exact: true })).toBeVisible();
  await page.locator('.cf-sidebar').getByRole('link', { name: 'Appointments', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Schedule Appointment', exact: true })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Edit appointment', exact: true })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'View appointment', exact: true })).toBeEnabled();
  await page.locator('.cf-sidebar').getByRole('link', { name: 'Pharmacy', exact: true }).click();
  await expect(page.locator('#addMedicineForm button[type=submit]')).toBeEnabled();
});

test('appointment dropdown only lists active doctors', async ({ page }) => {
  await mockApi(page);
  await page.route('**/api/staff', route => route.fulfill({ json: [
    { id: 1, name: 'Doctor One', position: 'Doctor', isActive: true },
    { id: 2, name: 'Pharmacy One', position: 'Pharmacist', isActive: true },
    { id: 3, name: 'Nurse One', position: 'Nurse', isActive: true },
    { id: 4, name: 'Lab One', position: 'Lab Technician', isActive: true },
    { id: 5, name: 'Reception One', position: 'Receptionist', isActive: true },
    { id: 6, name: 'Inactive Doctor', position: 'Doctor', isActive: false }
  ] }));
  await page.goto('/app/');
  await page.getByRole('button', { name: 'Schedule visit', exact: true }).click();
  await expect(page.locator('#appointmentDoctor option')).toHaveText(['Select Doctor', 'Doctor One - Doctor']);
});
