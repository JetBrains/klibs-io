import { test, expect } from '@playwright/test';

test.describe('Search bar', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/');
    });

    test('Search for the "Ktor" project is successful', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        await searchInput.fill('Ktor');
        await expect(searchInput).toHaveValue('Ktor');
        const ktorCard = page.getByRole('link', { name: 'Ktor' }).first();
        await expect(ktorCard).toBeVisible();
    });

    test('Click on the "Clear" button clears the search query', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        await searchInput.fill('koin');
        await expect(searchInput).toHaveValue('koin');
        const clearButton = page.getByText('Clear Esc');
        await expect(clearButton).toBeVisible();
        await clearButton.click();
        await expect(searchInput).toHaveValue('');
    });

    test('Check for the "Esc" button on keyboard clears the search query', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        await searchInput.fill('koin');
        await expect(searchInput).toHaveValue('koin');
        await page.keyboard.press('Escape');
        await expect(searchInput).toHaveValue('');
    });

    // A timeout is needed at the beginning.
    test('Keyboard shortcut "/" makes the search bar focused', async ({ page }) => {
        await page.waitForTimeout(2000);
        await page.keyboard.press('/');
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeFocused();
        await searchInput.fill('test');
        await expect(searchInput).toHaveValue('test');
    });

    test('Click on "Packages" option in search mode switcher loads package cards', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        const searchByMode = page.locator('[data-e2e="search-by-mode"]');
        await expect(searchByMode).toBeVisible();
        // Click on "Packages" option in the switcher
        await searchByMode.getByText('Packages').click();
        // Verify that package cards are visible (package links contain /package/ in the URL)
        const packageCard = page.locator('a[href^="/package/"]').first();
        await expect(packageCard).toBeVisible();
    });

    test('Click on "Filter" option opens a dropdown list with filters', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        const filterTrigger = page.getByTestId('platform-filter-dropdown');
        await expect(filterTrigger).toBeVisible();
        await filterTrigger.click();
        await page.waitForTimeout(2000);
        // Assert that all platform checkboxes from the dropdown list are visible
        await expect(page.getByRole('checkbox', { name: 'Android JVM' })).toBeVisible();
        await expect(page.getByRole('checkbox', { name: 'JVM', exact: true })).toBeVisible();
        await expect(page.getByRole('checkbox', { name: 'Kotlin/Native' })).toBeVisible();
        await expect(page.getByRole('checkbox', { name: 'Wasm' })).toBeVisible();
        await expect(page.getByRole('checkbox', { name: 'JS' })).toBeVisible();
    });

    test('Search by package: com.zegreatrob.testmints/action-annotation', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        const searchByMode = page.locator('[data-e2e="search-by-mode"]');
        await expect(searchByMode).toBeVisible();
        await searchByMode.getByText('Packages').click();
        await searchInput.fill('com.zegreatrob.testmints/action-annotation');
        await page.waitForTimeout(2000);
        const resultLink = page.getByRole('link', { name: 'com.zegreatrob.testmints:action-annotation' }).first();
        await expect(resultLink).toBeVisible();
    });
});
