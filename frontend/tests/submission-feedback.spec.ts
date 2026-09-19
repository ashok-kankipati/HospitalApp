import { test, expect } from '@playwright/test';
import { fileURLToPath } from 'node:url';

const script = fileURLToPath(new URL('../../src/main/resources/static/js/submission-feedback.js', import.meta.url));

test.beforeEach(async ({ page }) => {
  await page.route('**/submission-check', route => route.fulfill({ contentType: 'text/html', body: '<form><button>Submit</button></form>' }));
  await page.goto('/submission-check');
  await page.addScriptTag({ path: script });
  await page.evaluate(() => {
    document.querySelector('form')!.addEventListener('submit', event => {
      event.preventDefault();
      void fetch('/api/test-submit', { method: 'POST' }).then(r => r.json()).catch(() => {});
    });
  });
});

test('slow submission blocks repeated clicks and Enter until the body arrives', async ({ page }) => {
  let requests = 0;
  let release!: () => void;
  const hold = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/test-submit', async route => {
    requests++;
    await hold;
    await route.fulfill({ json: { ok: true } });
  });
  await page.getByRole('button').click();
  await expect(page.getByRole('status')).toContainText('Saving');
  await expect(page.getByRole('button')).toHaveAttribute('aria-busy', 'true');
  await page.evaluate(() => {
    document.querySelector('button')!.click();
    document.querySelector('button')!.click();
    document.querySelector('form')!.requestSubmit();
  });
  expect(requests).toBe(1);
  release();
  await expect(page.getByRole('status')).toBeHidden();
  await expect(page.getByRole('button')).not.toHaveAttribute('aria-busy');
  await page.getByRole('button').click();
  await expect.poll(() => requests).toBe(2);
});

test('network failure releases controls and reports uncertain result', async ({ page }) => {
  await page.route('**/api/test-submit', route => route.abort());
  await page.getByRole('button').click();
  await expect(page.getByRole('status')).toContainText('Check the result');
  await expect(page.getByRole('button')).not.toHaveAttribute('aria-busy');
});

test('server rejection releases controls and does not report success', async ({ page }) => {
  await page.route('**/api/test-submit', route => route.fulfill({ status: 409, json: { message: 'Already dispensed' } }));
  await page.getByRole('button').click();
  await expect(page.getByRole('status')).toContainText('could not be confirmed');
  await expect(page.getByRole('button')).not.toHaveAttribute('aria-busy');
});
