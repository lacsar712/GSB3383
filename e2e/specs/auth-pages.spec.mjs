import { test, expect } from '@playwright/test';
import { credentials } from '../helpers/data.mjs';
import { registerViaApi } from '../helpers/auth.mjs';

test.describe('鉴权与公开页面', () => {
  test('未登录访问受保护页面与接口应被拦截', async ({ page }) => {
    await page.goto('/cart');
    await expect(page).toHaveURL(/\/login$/);

    await page.goto('/orders');
    await expect(page).toHaveURL(/\/login$/);

    const apiResp = await page.request.post('/api/orders', {
      data: {
        contactName: '未登录用户',
        contactPhone: '13800000000',
        deliveryAddress: 'A栋 101',
      },
    });
    expect(apiResp.status()).toBe(401);
    const payload = await apiResp.json();
    expect(payload.success).toBeFalsy();
    expect(payload.error.code).toBe('UNAUTHORIZED');
  });

  test('注册页覆盖：密码不一致、成功注册、重复用户名', async ({ page }) => {
    const user = credentials('register');

    await page.goto('/register');
    await page.getByTestId('register-username').fill(user.username);
    await page.getByTestId('register-password').fill(user.password);
    await page.getByTestId('register-confirm-password').fill('Mismatch@123');
    await page.getByTestId('register-submit').click();
    await expect(page.getByTestId('register-error')).toContainText('两次输入的密码不一致');

    await page.getByTestId('register-confirm-password').fill(user.password);
    await page.getByTestId('register-submit').click();
    await expect(page.getByTestId('register-success')).toContainText('注册成功');
    await expect(page).toHaveURL(/\/login$/, { timeout: 5_000 });

    await page.goto('/register');
    await page.getByTestId('register-username').fill(user.username);
    await page.getByTestId('register-password').fill(user.password);
    await page.getByTestId('register-confirm-password').fill(user.password);
    await page.getByTestId('register-submit').click();
    await expect(page.getByTestId('register-error')).toContainText('用户名已存在');
  });

  test('登录页覆盖：错误密码与正确登录', async ({ page }) => {
    const user = credentials('login');
    await registerViaApi(page.request, user.username, user.password);

    await page.goto('/login');
    await page.getByTestId('login-username').fill(user.username);
    await page.getByTestId('login-password').fill('Wrong@123');
    await page.getByTestId('login-submit').click();
    await expect(page.getByTestId('login-error')).toContainText('用户名或密码错误');

    await page.getByTestId('login-password').fill(user.password);
    await page.getByTestId('login-submit').click();
    await expect(page).toHaveURL(/\/menu$/);

    await expect(page.getByTestId('nav-menu')).toBeVisible();
    await expect(page.getByTestId('nav-cart')).toBeVisible();
    await expect(page.getByTestId('nav-orders')).toBeVisible();
    await expect(page.getByTestId('logout-btn')).toBeVisible();
  });
});
