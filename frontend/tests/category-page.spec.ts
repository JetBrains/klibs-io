import { test, expect } from '@playwright/test';
import { MainPage } from './pages/MainPage';

test.describe('Category page', () => {
    test('Clear tag clears the category filter', async ({ page }) => {
        const mainPage = new MainPage(page);
        await page.goto('/?category=grant-winners');
        await mainPage.acceptCookiesIfPresent();

        const categoryClearTag = page.getByTestId('category-clear-tag');
        await expect(categoryClearTag).toBeVisible({ timeout: 10000 });
        await expect(categoryClearTag).toContainText(/grant winners/i);

        await categoryClearTag.click();
        await expect(page).toHaveURL('/');
        await expect(categoryClearTag).not.toBeVisible();
    });
    
    test('Search bar contains the "Search in Featured" text when the "Featured" category is selected', async ({ page }) => {
        await page.goto('/?category=featured');
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        await expect(searchInput).toHaveAttribute('placeholder', /Search in Featured/i);
    });

    test('Search bar contains the "Featured" tag when the "Featured" category is selected', async ({ page }) => {
        await page.goto('/?category=featured');
        const categoryTag = page.getByTestId('category-clear-tag');
        await expect(categoryTag).toBeVisible({ timeout: 10000 });
        await expect(categoryTag).toContainText("Featured");
    });
});
