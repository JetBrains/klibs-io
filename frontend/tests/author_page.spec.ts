import {expect, test} from "@playwright/test";


test.describe('Author page elements', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/author/mikepenz');

        if (process.env.PROD) {
            await page.waitForSelector('button.ch2-btn.ch2-btn-primary');
            await page.click('button.ch2-btn.ch2-btn-primary');
        }
    });

    test('Check the author name', async ({ page }) => {
        const name = page.getByTestId('author-name');
        await expect(name).toBeVisible();
    });

    test('Followers counter is visible', async ({ page }) => {
        const followers = page.getByTestId('author-followers');
        await expect(followers).toBeVisible();
        await expect(followers).toContainText('followers');
    });

    test('Location is visible when provided', async ({ page }) => {
        const location = page.getByTestId('author-location');
        // Location may be absent for some authors. Check visibility if present.
        if (await location.count()) {
            await expect(location).toBeVisible();
        }
    });

    test('Homepage link opens the author homepage', async ({ page, context }) => {
        const homepageLink = page.getByTestId('author-homepage-link');
        if (await homepageLink.count()) {
            await expect(homepageLink).toBeVisible();
            await homepageLink.click();
            const newPage = await context.waitForEvent('page');
            await newPage.waitForLoadState();
            await expect(newPage.url()).toMatch(/https?:\/\//);
        }
    });

    test('Twitter link opens the related page', async ({ page, context }) => {
        const xLink = page.getByTestId('author-twitter-link');
        if (await xLink.count()) {
            await expect(xLink).toBeVisible();
            await xLink.click();
            const newPage = await context.waitForEvent('page');
            await newPage.waitForLoadState();
            await expect(newPage.url()).toContain('https://x.com/');
        }
    });

    test('GitHub link opens author GitHub page', async ({ page, context }) => {
        const githubLink = page.getByTestId('author-github-login');
        await expect(githubLink).toBeVisible();
        await githubLink.click();
        const newPage = await context.waitForEvent('page');
        await newPage.waitForLoadState();
        await expect(newPage.url()).toContain('https://github.com/');
    });

    test('The author projects are visible', async ({ page }) => {
        const projects = page.getByTestId('author-projects');
        await expect(projects).toBeVisible();
    });
});