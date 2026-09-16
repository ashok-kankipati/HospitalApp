import { test, expect, type Page } from '@playwright/test';

async function setup(page: Page) {
  const writes: { path: string; body: Record<string, unknown> }[] = [];
  await page.route('**/api/**', route => {
    const path = new URL(route.request().url()).pathname;
    if (route.request().method() === 'POST') { const body = route.request().postDataJSON(); writes.push({ path, body }); return route.fulfill({ json: { ...body, id: 1 } }); }
    if (path === '/api/auth/session') return route.fulfill({ json: { username: 'Admin', role: 'Admin' } });
    if (path === '/api/beds/summary') return route.fulfill({ json: { total: 0, available: 0, occupied: 0 } });
    return route.fulfill({ json: [] });
  });
  return writes;
}

test('patient form blocks invalid values, preserves input and accepts international names', async ({ page }) => {
  const writes = await setup(page); await page.goto('/app/#/patients');
  await page.getByRole('button', { name: 'Add patient', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'A new care journey' });
  await dialog.getByLabel('Full name', { exact: true }).fill('   ');
  await dialog.getByLabel('Email address', { exact: true }).fill('person@invalid');
  await dialog.getByLabel('Phone number', { exact: true }).fill('123');
  await dialog.getByLabel('Date of birth', { exact: true }).fill('2099-01-01');
  await dialog.getByLabel('Address', { exact: true }).fill('---');
  await dialog.getByRole('button', { name: 'Add patient', exact: true }).click();
  await expect(dialog.locator('[aria-invalid="true"]')).toHaveCount(5);
  expect(writes).toHaveLength(0);
  await expect(dialog.getByLabel('Full name', { exact: true })).toBeFocused();
  await dialog.getByLabel('Full name', { exact: true }).fill('  María José  ');
  await dialog.getByLabel('Email address', { exact: true }).fill('maria+care@example.test');
  await dialog.getByLabel('Phone number', { exact: true }).fill('+91 (98765) 43210');
  await dialog.getByLabel('Date of birth', { exact: true }).fill('2000-02-29');
  await dialog.getByLabel('Address', { exact: true }).fill('12 Main Road, Hyderabad');
  await dialog.getByRole('button', { name: 'Add patient', exact: true }).click();
  await expect(dialog).not.toBeVisible();
  expect(writes[0].body.name).toBe('María José');
});

test('dynamic pharmacy form rejects expired stock and fractional quantities', async ({ page }) => {
  const writes = await setup(page);
  await page.route('**/api/pharmacy/medicines', route => route.fulfill({ json: [{ id: 1, name: 'Medicine 1' }] }));
  await page.goto('/app/#/pharmacy');
  await page.locator('.pharmacy-entry summary').filter({ hasText: 'Receive stock' }).click();
  const form = page.locator('#addBatchForm');
  await expect(form).toBeAttached();
  await expect(page.locator('#batchMedicine option[value="1"]')).toBeAttached();
  await page.locator('#batchMedicine').selectOption('1', { force: true });
  await page.locator('#batchNo').fill('B1', { force: true });
  await page.locator('#batchExpiry').fill('2000-01-01', { force: true });
  await page.locator('#batchQuantity').fill('1.5', { force: true });
  await page.locator('#batchPrice').fill('-1', { force: true });
  await form.evaluate(form => (form as HTMLFormElement).requestSubmit());
  await expect(page.locator('#batchExpiry')).toHaveAttribute('aria-invalid', 'true');
  await expect(page.locator('#batchQuantity')).toHaveAttribute('aria-invalid', 'true');
  await expect(page.locator('#batchPrice')).toHaveAttribute('aria-invalid', 'true');
  expect(writes).toHaveLength(0);
});

test('account passwords must match and fit the server byte limit', async ({ page }) => {
  const writes = await setup(page); await page.goto('/app/#/account');
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Username', { exact: true }).fill('doctor');
  await dialog.getByLabel('Email', { exact: true }).fill('doctor@example.test');
  await dialog.getByLabel('Password', { exact: true }).fill('Strong-pass');
  await dialog.getByLabel('Confirm password', { exact: true }).fill('different');
  await dialog.getByRole('button', { name: 'Save account' }).click();
  await expect(dialog.getByText('Passwords must match.', { exact: true })).toBeVisible();
  expect(writes).toHaveLength(0);
  await dialog.getByLabel('Password', { exact: true }).fill('é'.repeat(40));
  await dialog.getByLabel('Confirm password', { exact: true }).fill('é'.repeat(40));
  await dialog.getByRole('button', { name: 'Save account' }).click();
  await expect(dialog.locator('[name="password"]')).toHaveAttribute('aria-invalid', 'true');
  expect(writes).toHaveLength(0);
});
