import { test, expect } from '@playwright/test';

test.describe('Project pages labels', () => {
    test.beforeEach(async ({ page }) => {

        if (process.env.PROD) {
            await page.waitForSelector('button.ch2-btn.ch2-btn-primary');
            await page.click('button.ch2-btn.ch2-btn-primary');
        }
    });

    test('Ktor page should display Featured project label', async ({ page }) => {
        await page.goto('/project/ktorio/ktor');
        await expect(page.getByText('Featured project').nth(1)).toBeVisible();
    });

    test('Compose-rich-editor page should display Kotlin grant winner label', async ({ page }) => {
        await page.goto('/project/MohamedRejeb/compose-rich-editor');
        await expect(page.getByText('Kotlin grant winner').nth(1)).toBeVisible();
    });

    test('Coil page should display both Featured project and Kotlin grant winner labels', async ({ page }) => {
        await page.goto('/project/coil-kt/coil');
        await expect(page.getByText('Featured project').nth(1)).toBeVisible();
        await expect(page.getByText('Kotlin grant winner').nth(1)).toBeVisible();
    });
});
