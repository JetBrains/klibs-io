import { test, expect } from '@playwright/test';
import { acceptCookiesIfPresent } from './helpers';

test.describe('Library discovery', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/');
        await acceptCookiesIfPresent(page);
    });

    test('finds and opens a project', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await searchInput.fill('Ktor');
        await expect(page).toHaveURL(/\?query=Ktor/);
        await page.getByRole('link', { name: 'Ktor' }).first().click();

        await expect(page).toHaveURL(/\/project\/[^/]+\/[^/?#]+/);
    });

    test('finds and opens a package', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        const searchByMode = page.locator('[data-e2e="search-by-mode"]');
        await searchByMode.getByText('Packages').click();
        await searchInput.fill('com.zegreatrob.testmints/action-annotation');
        await page.getByRole('link', { name: 'com.zegreatrob.testmints:action-annotation' }).first().click();

        await expect(page).toHaveURL(/\/package\/com\.zegreatrob\.testmints\/action-annotation/);
    });
});
