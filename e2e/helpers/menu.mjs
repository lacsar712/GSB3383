import { expect } from '@playwright/test';

export async function addFirstAvailableMenuItem(page, quantity = 1) {
  await page.goto('/menu');
  const addButtons = page.locator('[data-testid^="menu-add-"]:not([disabled])');
  await expect(addButtons.first()).toBeVisible();
  for (let i = 0; i < quantity; i += 1) {
    await addButtons.first().click();
  }
  await expect(page.getByTestId('menu-success')).toContainText('已加入购物车');
}

export async function addMenuItemByName(page, keyword) {
  await page.goto('/menu');
  await page.getByTestId('menu-search-input').fill(keyword);
  await page.getByTestId('menu-search-submit').click();
  const addButtons = page.locator('[data-testid^="menu-add-"]:not([disabled])');
  await expect(addButtons.first()).toBeVisible();
  await addButtons.first().click();
  await expect(page.getByTestId('menu-success')).toContainText('已加入购物车');
}
