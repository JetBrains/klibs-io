import { test, expect } from '@playwright/test';
import { acceptCookiesIfPresent } from './helpers';

test.describe('Unknown route', () => {
    test('shows the not-found page', async ({ page }) => {
        await page.goto('/');
        await acceptCookiesIfPresent(page);

        await page.goto('/not-found-page');

        await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible();
    });
});
