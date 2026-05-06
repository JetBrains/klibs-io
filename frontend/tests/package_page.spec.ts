import { test, expect } from '@playwright/test';

test.describe('Package page (OkCurl)', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/package/com.squareup.okhttp3/okcurl');

        if (process.env.PROD) {
            await page.waitForSelector('button.ch2-btn.ch2-btn-primary');
            await page.click('button.ch2-btn.ch2-btn-primary');
        }
    });

    test('Package description for the "okcurl" is not empty', async ({ page }) => {
        const description = page.getByTestId("package-description");
        await expect(description).toBeVisible();
        await expect(description).not.toBeEmpty();
    });

    test('Click on "Homepage" button opens the github page of the package', async ({ page }) => {
        const packageHomepageButton = page.getByRole('link', { name: 'Homepage' });
        await expect(packageHomepageButton).toBeVisible();
        await packageHomepageButton.click();
        await expect(page).toHaveURL('https://square.github.io/okhttp/');
    });

    test('Click on "Source code management" button opens the GitHub repository', async ({ page }) => {
        const scmLink = page.getByRole('link', { name: 'Source code management' });
        await expect(scmLink).toBeVisible();
        await scmLink.click();
        await expect(page).toHaveURL('https://github.com/square/okhttp');
    });

    // Added a timeout because without it the test fails. The page doesn't have time to open.
    test('Click on "Maven artifacts" button opens the artifact files on Maven Central', async ({ page }) => {
        const filesLink = page.getByRole('link', { name: 'Maven artifacts' });
        await expect(filesLink).toBeVisible();
        await page.waitForTimeout(4000)
        await filesLink.click();
        await expect(page.url()).toContain('https://repo1.maven.org/maven2/com/squareup/okhttp3/okcurl');
    });
});



