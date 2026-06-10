import { test, expect } from '@playwright/test';
import { credentials } from '../helpers/data.mjs';
import { registerViaApi, loginViaUi } from '../helpers/auth.mjs';
import { addFirstAvailableMenuItem } from '../helpers/menu.mjs';

async function firstCartItemId(page) {
  const row = page.locator('[data-testid^="cart-row-"]').first();
  await expect(row).toBeVisible();
  const testId = await row.getAttribute('data-testid');
  if (!testId) {
    throw new Error('cart row test id missing');
  }
  return testId.replace('cart-row-', '');
}

test.describe('用户端菜单/购物车/订单全链路', () => {
  test('菜单页分支：搜索下架项、清空搜索、可售菜品加购', async ({ page }) => {
    const user = credentials('menu');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await page.goto('/menu');
    await page.getByTestId('menu-search-input').fill('香辣鸡翅');
    await page.getByTestId('menu-search-submit').click();

    await expect(page.getByTestId('menu-list')).toContainText('暂无符合条件的菜品');
    await expect(page.locator('[data-testid^="menu-add-"][disabled]')).toHaveCount(0);

    await page.getByTestId('menu-search-clear').click();
    const enabledButton = page.locator('[data-testid^="menu-add-"]:not([disabled])').first();
    await expect(enabledButton).toBeVisible();

    await enabledButton.click();
    await expect(page.getByTestId('menu-success')).toContainText('已加入购物车');
  });

  test('购物车页分支/边界：负数、更新、置零删除、空购物车下单失败', async ({ page }) => {
    const user = credentials('cart');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await addFirstAvailableMenuItem(page, 1);

    await page.goto('/cart');
    const cartId = await firstCartItemId(page);

    await page.getByTestId(`cart-qty-${cartId}`).fill('-1');
    await page.getByTestId(`cart-update-${cartId}`).click();
    await expect(page.getByTestId('cart-error')).toContainText('数量必须为大于等于 0 的整数');

    await page.getByTestId(`cart-qty-${cartId}`).fill('2');
    await page.getByTestId(`cart-update-${cartId}`).click();
    await expect(page.getByTestId('cart-success')).toContainText('数量更新成功');
    await expect(page.getByTestId('cart-total')).toHaveText(/\d+\.\d{2}/);

    await page.getByTestId(`cart-qty-${cartId}`).fill('0');
    await page.getByTestId(`cart-update-${cartId}`).click();
    await expect(page.getByTestId('cart-success')).toContainText('商品已从购物车移除');
    await expect(page.getByTestId('cart-body')).toContainText('购物车为空');

    await page.getByTestId('order-contact-name').fill('张三');
    await page.getByTestId('order-contact-phone').fill('13800000000');
    await page.getByTestId('order-delivery-address').fill('教学楼 A101');
    await page.getByTestId('order-submit').click();
    await expect(page.getByTestId('cart-error')).toContainText('购物车为空');
  });

  test('下单成功链路：菜单->购物车->订单页，并校验返回结构', async ({ page }) => {
    const user = credentials('order');
    await registerViaApi(page.request, user.username, user.password);
    await loginViaUi(page, user.username, user.password);

    await addFirstAvailableMenuItem(page, 2);

    await page.goto('/cart');
    await page.getByTestId('order-contact-name').fill('李四');
    await page.getByTestId('order-contact-phone').fill('13900000000');
    await page.getByTestId('order-delivery-address').fill('实验楼 B202');
    await page.getByTestId('order-submit').click();

    await expect(page).toHaveURL(/\/orders$/);
    const firstOrder = page.locator('[data-testid^="order-card-"]').first();
    await expect(firstOrder).toBeVisible();
    await expect(firstOrder.locator('[data-testid^="order-status-"]')).toContainText('PLACED');
    await expect(firstOrder.locator('[data-testid^="order-item-"]').first()).toBeVisible();

    const response = await page.request.get('/api/orders?includeItems=true');
    expect(response.ok()).toBeTruthy();
    const payload = await response.json();

    expect(payload.success).toBeTruthy();
    expect(Array.isArray(payload.data)).toBeTruthy();
    expect(payload.data.length).toBeGreaterThan(0);

    const order = payload.data[0];
    expect(typeof order.id).toBe('number');
    expect(typeof order.status).toBe('string');
    expect(typeof order.createdAt).toBe('string');
    expect(typeof order.updatedAt).toBe('string');
    expect(Array.isArray(order.items)).toBeTruthy();
    expect(order.items.length).toBeGreaterThan(0);
  });
});
