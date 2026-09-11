import { Page } from '@playwright/test';

export async function acceptCookiesIfPresent(page: Page) {
    if (process.env.PROD) {
        await page.locator('.ch2-allow-all-btn').click();
    }
}
