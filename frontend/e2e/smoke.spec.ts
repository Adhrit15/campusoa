import { expect, test } from '@playwright/test';

test('home → drives → company page', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Placement prep, together.' })).toBeVisible();
  // ground rule: the disclaimer is on every page
  await expect(page.getByText('is not affiliated with or endorsed by CBIT').first()).toBeVisible();

  // view drives (mobile bottom nav)
  await page.getByRole('navigation', { name: 'Mobile' }).getByRole('link', { name: 'Drives' }).click();
  await expect(page.getByRole('heading', { name: 'Drive calendar' })).toBeVisible();
  await expect(page.getByText(/\d+ drives/)).toBeVisible();

  // switch to the calendar view and back
  await page.getByRole('tab', { name: 'calendar' }).click();
  await expect(page.getByRole('button', { name: 'Next month' })).toBeVisible();

  // open a company page
  await page.goto('/companies');
  await page.getByRole('link', { name: 'Microsoft', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Microsoft' })).toBeVisible();
  await expect(page.getByText('51 LPA')).toBeVisible();
  await expect(page.getByText('Source: CBIT public placement records (approx.)').first()).toBeVisible();
});
