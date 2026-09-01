import { test, expect } from '@playwright/test';
import { acceptCookiesIfPresent } from './helpers';

test.describe('Category discovery', () => {
    test('browses grant winners and returns to all projects', async ({ page }) => {
        await page.goto('/');
        await acceptCookiesIfPresent(page);

        await page.getByTestId('grant-winners-discover-button').click();
        await expect(page).toHaveURL(/\?category=grant-winners/);
        await page.waitForLoadState('networkidle');

        const categoryFilter = page.getByTestId('category-clear-tag');
        await expect(categoryFilter).toContainText(/grant winners/i);
        await categoryFilter.click();

        await expect(page).toHaveURL(/\/$/);
        await expect(categoryFilter).not.toBeVisible();
    });
});
