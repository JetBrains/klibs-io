import { test, expect } from '@playwright/test';

test.describe('Not found page', () => {
      test.beforeEach(async ({ page }) => {
        await page.goto('/');

        if (process.env.PROD) {
          await page.waitForSelector('button.ch2-btn.ch2-btn-primary');
          await page.click('button.ch2-btn.ch2-btn-primary');
        }
      });

    // Not found URL.
    test('Shows the 404 page message for a non-existing URL', async ({ page }) => {
        await page.goto('/not-found-page');
        await expect(page.getByTestId('not-found-page-message')).toBeVisible();
    });

    // Not found project.
    test('Shows the no search results message for a non-existent project', async ({ page }) => {
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        //  A timeout is needed for the first loading the page.
        await page.waitForTimeout(2000);
        await searchInput.fill('proj12345');
        await page.waitForTimeout(2000);
        await expect(searchInput).toHaveValue('proj12345');
        await expect(page.getByTestId('search-no-results-message')).toBeVisible();
    });

    // Not found package.
    test('Shows the no search results for a non-existent package', async ({ page }) => {
        await page.goto('/?mode=packages');
        const searchInput = page.getByTestId('search-input');
        await expect(searchInput).toBeVisible();
        await page.waitForTimeout(2000);
        await searchInput.fill('pack12345');
        await page.waitForTimeout(2000);
        await expect(searchInput).toHaveValue('pack12345')
        await expect(page.getByTestId('search-no-results-message')).toBeVisible();
    });
});

