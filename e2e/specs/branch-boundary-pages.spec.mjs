import { test, expect } from '@playwright/test';
import { credentials } from '../helpers/data.mjs';
import { registerViaApi, loginViaUi, loginAdmin, logout } from '../helpers/auth.mjs';
import { addFirstAvailableMenuItem } from '../helpers/menu.mjs';

function parseOrderIdFromTitle(text) {
  const match = text.match(/订单\s+#(\d+)/);
  if (!match) {
    throw new Error(`Failed to parse order id from: ${text}`);
  }
  return match[1];
}

async function firstCartItemId(page) {
  const row = page.locator('[data-testid^="cart-row-"]').first();
  await expect(row).toBeVisible();
  const testId = await row.getAttribute('data-testid');
  if (!testId) {
    throw new Error('Missing cart row test id');
  }
  return testId.replace('cart-row-', '');
}

test.describe('页面分支与边界补充覆盖', () => {
  test('首页路由与退出分支：未登录跳转登录，登录后跳转菜单，退出后会话失效', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);

    const user = credentials('root');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await page.goto('/');
    await expect(page).toHaveURL(/\/menu$/);

    await logout(page);
    await page.goto('/menu');
    await expect(page).toHaveURL(/\/login$/);

    const apiResp = await page.request.get('/api/cart');
    expect(apiResp.status()).toBe(401);
    const payload = await apiResp.json();
    expect(payload.error.code).toBe('UNAUTHORIZED');
  });

  test('菜单页分支：无结果空态、清空恢复、下架菜品接口加购被拒绝', async ({ page }) => {
    const user = credentials('menu_edge');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await page.goto('/menu');
    await page.getByTestId('menu-search-input').fill('香辣鸡翅');
    await page.getByTestId('menu-search-submit').click();
    await expect(page.getByTestId('menu-list')).toContainText('暂无符合条件的菜品');

    await page.getByTestId('menu-search-clear').click();
    await expect(page.locator('[data-testid^="menu-card-"]').first()).toBeVisible();

    const unavailableResp = await page.request.post('/api/cart/items', {
      data: { menuItemId: 5, quantity: 1 },
    });
    expect(unavailableResp.status()).toBe(400);
    const unavailablePayload = await unavailableResp.json();
    expect(unavailablePayload.error.code).toBe('MENU_ITEM_UNAVAILABLE');
  });

  test('购物车页边界：非整数数量前端拦截、删除按钮路径、删除后再次删除返回 404', async ({ page }) => {
    const user = credentials('cart_edge');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await addFirstAvailableMenuItem(page, 1);
    await page.goto('/cart');
    const cartItemId = await firstCartItemId(page);

    await page.getByTestId(`cart-qty-${cartItemId}`).fill('1.5');
    await page.getByTestId(`cart-update-${cartItemId}`).click();
    await expect(page.getByTestId('cart-error')).toContainText('数量必须为大于等于 0 的整数');

    await page.getByTestId(`cart-delete-${cartItemId}`).click();
    await expect(page.getByTestId('cart-success')).toContainText('已删除购物车项');
    await expect(page.getByTestId('cart-body')).toContainText('购物车为空');

    const deleteAgainResp = await page.request.delete(`/api/cart/items/${cartItemId}`);
    expect(deleteAgainResp.status()).toBe(404);
    const deleteAgainPayload = await deleteAgainResp.json();
    expect(deleteAgainPayload.error.code).toBe('NOT_FOUND');
  });

  test('订单页分支：空订单列表与下单后明细渲染', async ({ page }) => {
    const user = credentials('orders_edge');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await page.goto('/orders');
    await expect(page.getByTestId('orders-list')).toContainText('暂无订单');

    await addFirstAvailableMenuItem(page, 1);
    await page.goto('/cart');
    await page.getByTestId('order-contact-name').fill('王五');
    await page.getByTestId('order-contact-phone').fill('13600000000');
    await page.getByTestId('order-delivery-address').fill('图书馆 1F');
    await page.getByTestId('order-submit').click();

    await expect(page).toHaveURL(/\/orders$/);
    const orderTitle = await page.locator('[data-testid^="order-title-"]').first().innerText();
    const orderId = parseOrderIdFromTitle(orderTitle);
    await expect(page.locator(`[data-testid^="order-item-${orderId}-"]`).first()).toBeVisible();
    await expect(page.getByTestId(`order-contact-${orderId}`)).toContainText('王五');
    await expect(page.getByTestId(`order-total-${orderId}`)).toHaveText(/¥ \d+\.\d{2}/);
  });

  test('管理员边界：普通用户访问管理 API 返回 403，非法状态值返回 400，空筛选显示暂无订单', async ({ browser, page }) => {
    const normalUser = credentials('forbidden_api');
    await registerViaApi(page.request, normalUser.username, normalUser.password);
    await loginViaUi(page, normalUser.username, normalUser.password);

    const forbiddenResp = await page.request.get('/api/admin/orders');
    expect(forbiddenResp.status()).toBe(403);
    const forbiddenPayload = await forbiddenResp.json();
    expect(forbiddenPayload.error.code).toBe('FORBIDDEN');
    await logout(page);

    const buyer = credentials('status_edge');
    await registerViaApi(page.request, buyer.username, buyer.password);
    const userContext = await browser.newContext({ baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:8080' });
    const userPage = await userContext.newPage();
    let orderId;
    try {
      await loginViaUi(userPage, buyer.username, buyer.password);
      await addFirstAvailableMenuItem(userPage, 1);
      await userPage.goto('/cart');
      await userPage.getByTestId('order-contact-name').fill('测试用户');
      await userPage.getByTestId('order-contact-phone').fill('13500000000');
      await userPage.getByTestId('order-delivery-address').fill('测试地址 100 号');
      await userPage.getByTestId('order-submit').click();
      await expect(userPage).toHaveURL(/\/orders$/);
      const orderTitle = await userPage.locator('[data-testid^="order-title-"]').first().innerText();
      orderId = parseOrderIdFromTitle(orderTitle);
    } finally {
      await userContext.close();
    }

    await loginAdmin(page);
    const invalidStatusResp = await page.request.put(`/api/admin/orders/${orderId}/status`, {
      data: { status: 'INVALID_STATUS' },
    });
    expect(invalidStatusResp.status()).toBe(400);
    const invalidStatusPayload = await invalidStatusResp.json();
    expect(invalidStatusPayload.error.code).toBe('INVALID_STATUS');

    await page.goto('/admin/orders');
    await page.getByTestId('admin-orders-filter').selectOption('CANCELED');
    await page.getByTestId('admin-orders-filter-submit').click();
    await expect(page.getByTestId('admin-orders-list')).toContainText('暂无订单');
  });
});
