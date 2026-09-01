import { test, expect } from '@playwright/test';
import { acceptCookiesIfPresent } from './helpers';

test.describe('Project packages', () => {
    test('opens a package from a project', async ({ page }) => {
        await page.goto('/project/arrow-kt/arrow');
        await acceptCookiesIfPresent(page);

        await page.getByRole('tab', { name: 'Packages' }).first().click();
        const packageLink = page.locator('a[href^="/package/"]').first();
        await expect(packageLink).toBeVisible();
        await packageLink.click();

        await expect(page).toHaveURL(/\/package\/[^/]+\/[^/?#]+/);
    });
});
