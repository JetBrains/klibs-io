import { Page } from '@playwright/test';

export async function acceptCookiesIfPresent(page: Page) {
    if (process.env.PROD) {
        await page.locator('button.ch2-btn.ch2-btn-primary').click();
    }
}
