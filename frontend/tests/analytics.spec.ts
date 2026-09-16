import { test, expect } from '@playwright/test';

test('analytics aggregates visits, paid revenue, distinct patients and adjusted stock', async ({ page }) => {
  const today = new Date();
  const day = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  await page.route('**/api/**', route => {
    const path = new URL(route.request().url()).pathname;
    const data: Record<string, unknown> = {
      '/api/auth/session': { username: 'Admin', role: 'Admin' },
      '/api/staff': [{ id: 1, name: 'Dr Meera', position: 'Doctor', department: 'Cardiology' }],
      '/api/beds/summary': { total: 10, occupied: 4, available: 6 },
      '/api/visits': [{ patientId: 1, doctorId: 1, createdAt: day + 'T10:00:00' }, { patientId: 1, doctorId: 1, createdAt: day + 'T11:00:00' }],
      '/api/appointments': [{ id: 1, patientId: 1, staffId: 1, appointmentDate: day, appointmentTime: '10:00', status: 'COMPLETED' }, { id: 2, patientId: 1, staffId: 1, appointmentDate: day, appointmentTime: '11:00', status: 'CANCELLED' }],
      '/api/billing/invoices/payments': [{ amount: 250, paymentStatus: 'PAID', paymentMethod: 'CASH', paidAt: day + 'T10:00:00' }, { amount: 900, paymentStatus: 'PENDING', paymentMethod: 'UPI', paidAt: day + 'T10:00:00' }],
      '/api/pharmacy/batches': [{ id: 1, medicine: { name: 'Test medicine' }, batchNo: 'B1', quantity: 20, expiryDate: '2099-01-01', isActive: true }],
      '/api/pharmacy/transactions': [{ batch: { id: 1 }, quantity: 15, transactionType: 'OUT' }],
    };
    return route.fulfill({ json: data[path] || [] });
  });
  await page.goto('/app/');
  const analytics = page.getByRole('region', { name: 'Hospital analytics', exact: true });
  await expect(analytics.locator('.ca-card')).toHaveCount(8);
  await expect(page.getByRole('region', { name: 'Patient visits trend', exact: true }).getByRole('img')).toHaveAttribute('aria-label', new RegExp(`${day.slice(5)}: 2`));
  await expect(page.getByRole('region', { name: 'Department-wise patients', exact: true }).locator('.ca-bars strong')).toHaveText('1');
  await expect(page.getByRole('region', { name: 'Doctor workload', exact: true }).locator('.ca-bars strong')).toHaveText('1');
  await expect(page.getByRole('region', { name: 'Payment methods', exact: true })).toContainText('250.00');
  await expect(page.getByRole('region', { name: 'Payment methods', exact: true })).not.toContainText('UPI');
  await expect(page.getByRole('region', { name: 'Bed occupancy', exact: true })).toContainText('40.0%');
  await expect(analytics.getByRole('row').filter({ hasText: 'Test medicine' })).toContainText('Low stock');
  await expect(analytics.getByRole('row').filter({ hasText: 'Test medicine' }).getByRole('cell').nth(1)).toHaveText('5');
  await page.getByLabel('Reporting period').selectOption('7');
  await expect(page.getByRole('region', { name: 'Patient visits trend', exact: true }).locator('circle')).toHaveCount(7);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test('analytics exposes failures instead of showing zero values', async ({ page }) => {
  await page.route('**/api/**', route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/auth/session') return route.fulfill({ json: { username: 'Admin', role: 'Admin' } });
    if (path === '/api/visits') return route.fulfill({ status: 500, json: {} });
    return route.fulfill({ json: path === '/api/beds/summary' ? { total: 0, occupied: 0, available: 0 } : [] });
  });
  await page.goto('/app/');
  await expect(page.getByRole('region', { name: 'Patient visits trend', exact: true }).getByRole('alert')).toContainText('Unable to load');
  await expect(page.getByRole('region', { name: 'Bed occupancy', exact: true })).toContainText('No beds configured');
  await expect(page.getByRole('region', { name: 'Payment methods', exact: true })).toContainText('No records in this period');
});
