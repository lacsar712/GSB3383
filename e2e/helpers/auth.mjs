import { expect } from '@playwright/test';

export async function registerViaApi(request, username, password) {
  const response = await request.post('/api/auth/register', {
    data: { username, password },
  });
  expect(response.ok()).toBeTruthy();
  const payload = await response.json();
  expect(payload.success).toBeTruthy();
  return payload;
}

export async function loginViaUi(page, username, password) {
  await page.goto('/login');
  await page.getByTestId('login-username').fill(username);
  await page.getByTestId('login-password').fill(password);
  await page.getByTestId('login-submit').click();
  await expect(page).toHaveURL(/\/menu$/);
}

export async function loginAdmin(page) {
  await loginViaUi(page, 'admin', 'Admin@123');
  await expect(page.getByTestId('nav-admin-menu')).toBeVisible();
  await expect(page.getByTestId('nav-admin-orders')).toBeVisible();
}

export async function logout(page) {
  await page.getByTestId('logout-btn').click();
  await expect(page).toHaveURL(/\/login$/);
}
