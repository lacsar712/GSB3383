import { test, expect } from '@playwright/test';
import { credentials, uniqueId } from '../helpers/data.mjs';
import { loginAdmin, loginViaUi, registerViaApi } from '../helpers/auth.mjs';
import { addFirstAvailableMenuItem } from '../helpers/menu.mjs';

function parseOrderIdFromTitle(text) {
  const match = text.match(/订单\s+#(\d+)/);
  if (!match) {
    throw new Error(`Failed to parse order id from: ${text}`);
  }
  return match[1];
}

test.describe('管理员页面与权限分支', () => {
  test('普通用户访问管理员页面应返回 403', async ({ page }) => {
    const user = credentials('forbidden');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    const response = await page.goto('/admin/menu');
    expect(response).not.toBeNull();
    expect(response.status()).toBe(403);
  });

  test('后台菜品管理：新增、非法价格、合法更新与上下架', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/menu');

    const name = `自动化菜品-${uniqueId('dish')}`;
    await page.getByTestId('admin-menu-create-name').fill(name);
    await page.getByTestId('admin-menu-create-description').fill('e2e 自动化创建菜品');
    await page.getByTestId('admin-menu-create-price').fill('32.50');
    await page.getByTestId('admin-menu-create-image-url').fill('https://picsum.photos/seed/e2e-dish/640/360');
    await page.getByTestId('admin-menu-create-submit').click();
    await expect(page.getByTestId('admin-menu-success')).toContainText('新增菜品成功');

    const menuResponse = await page.request.get('/api/menu-items');
    expect(menuResponse.ok()).toBeTruthy();
    const menuPayload = await menuResponse.json();
    const created = (menuPayload.data || []).find((item) => item.name === name);
    if (!created) {
      throw new Error(`Failed to resolve created menu item by name: ${name}`);
    }
    const itemId = String(created.id);

    await expect(page.getByTestId(`admin-menu-price-${itemId}`)).toBeVisible();

    await page.getByTestId(`admin-menu-price-${itemId}`).fill('-1');
    await page.getByTestId(`admin-menu-save-${itemId}`).click();
    await expect(page.getByTestId('admin-menu-error')).toContainText('价格 必须大于 0');

    await page.getByTestId(`admin-menu-price-${itemId}`).fill('abc');
    await page.getByTestId(`admin-menu-save-${itemId}`).click();
    await expect(page.getByTestId('admin-menu-error')).toContainText('价格 必须是数字');

    await page.getByTestId(`admin-menu-price-${itemId}`).fill('29.90');
    await page.getByTestId(`admin-menu-avail-${itemId}`).uncheck();
    await page.getByTestId(`admin-menu-save-${itemId}`).click();
    await expect(page.getByTestId('admin-menu-success')).toContainText(`菜品 #${itemId} 已更新`);
    await expect(page.getByTestId(`admin-menu-avail-${itemId}`)).not.toBeChecked();
  });

  test('后台订单管理：筛选、状态流转、非法流转、用户侧状态同步', async ({ browser, page }) => {
    const user = credentials('buyer');
    await registerViaApi(page.request, user.username, user.password);

    const userContext = await browser.newContext({ baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:8080' });
    const userPage = await userContext.newPage();

    try {
      await loginViaUi(userPage, user.username, user.password);
      await addFirstAvailableMenuItem(userPage, 1);
      await userPage.goto('/cart');
      await userPage.getByTestId('order-contact-name').fill('购买用户');
      await userPage.getByTestId('order-contact-phone').fill('13700000000');
      await userPage.getByTestId('order-delivery-address').fill('学生公寓 2-303');
      await userPage.getByTestId('order-submit').click();
      await expect(userPage).toHaveURL(/\/orders$/);

      const orderTitle = await userPage.locator('[data-testid^="order-title-"]').first().innerText();
      const orderId = parseOrderIdFromTitle(orderTitle);
      await expect(userPage.getByTestId(`order-status-${orderId}`)).toContainText('PLACED');

      await loginAdmin(page);
      await page.goto('/admin/orders');
      await page.getByTestId('admin-orders-filter').selectOption('PLACED');
      await page.getByTestId('admin-orders-filter-submit').click();

      await expect(page.getByTestId(`admin-order-card-${orderId}`)).toBeVisible();
      await page.getByTestId(`admin-order-confirm-${orderId}`).click();
      await expect(page.getByTestId('admin-orders-success')).toContainText(`订单 #${orderId} 状态已更新为 CONFIRMED`);
      await page.getByTestId('admin-orders-filter').selectOption('CONFIRMED');
      await page.getByTestId('admin-orders-filter-submit').click();
      await expect(page.getByTestId(`admin-order-card-${orderId}`)).toBeVisible();
      await expect(page.getByTestId(`admin-order-status-${orderId}`)).toContainText('CONFIRMED');

      await userPage.reload();
      await expect(userPage.getByTestId(`order-status-${orderId}`)).toContainText('CONFIRMED');

      await page.getByTestId(`admin-order-complete-${orderId}`).click();
      await expect(page.getByTestId('admin-orders-success')).toContainText(`订单 #${orderId} 状态已更新为 COMPLETED`);
      await page.getByTestId('admin-orders-filter').selectOption('COMPLETED');
      await page.getByTestId('admin-orders-filter-submit').click();
      await expect(page.getByTestId(`admin-order-card-${orderId}`)).toBeVisible();
      await expect(page.getByTestId(`admin-order-status-${orderId}`)).toContainText('COMPLETED');

      await userPage.reload();
      await expect(userPage.getByTestId(`order-status-${orderId}`)).toContainText('COMPLETED');

      const invalidTransitionResp = await page.request.put(`/api/admin/orders/${orderId}/status`, {
        data: { status: 'CONFIRMED' },
      });
      expect(invalidTransitionResp.status()).toBe(400);
      const invalidPayload = await invalidTransitionResp.json();
      expect(invalidPayload.error.code).toBe('INVALID_STATUS_TRANSITION');

      await page.getByTestId('admin-orders-filter').selectOption('CANCELED');
      await page.getByTestId('admin-orders-filter-submit').click();
      await expect(page.getByTestId('admin-orders-list')).toBeVisible();
    } finally {
      await userContext.close();
    }
  });
});
