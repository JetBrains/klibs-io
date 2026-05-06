import {expect, test} from "@playwright/test";


test.describe('JetBrains organisation page elements', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/organization/JetBrains');

        if (process.env.PROD) {
            await page.waitForSelector('button.ch2-btn.ch2-btn-primary');
            await page.click('button.ch2-btn.ch2-btn-primary');
        }
    });

    test('Check the organization name', async ({ page }) => {
        const orgName = page.getByTestId('organization-name');
        await expect(orgName).toBeVisible();
    });

    test('Check the organization description is not empty', async ({ page }) => {
        const orgDescription = page.getByTestId('organization-description');
        await expect(orgDescription).toBeVisible();
        await expect(orgDescription).not.toBeEmpty();
    });

    test('Homepage link opens the Open Source JetBrains page', async ({ page, context }) => {
        const homepageLink = page.getByTestId('organization-homepage-link');
        await expect(homepageLink).toBeVisible();
        await homepageLink.click();
        const newPagePromise = context.waitForEvent('page');
        const newPage = await newPagePromise;
        await newPage.waitForLoadState();
        await expect(newPage.url()).toContain('https://www.jetbrains.com/opensource');
    });

    test('Twitter link opens JetBrains Twitter', async ({ page, context }) => {
        const twitterLink = page.getByTestId('organization-twitter-link');
        await expect(twitterLink).toBeVisible();
        await twitterLink.click();
        const newPagePromise = context.waitForEvent('page');
        const newPage = await newPagePromise;
        await newPage.waitForLoadState();
        await expect(newPage.url()).toContain('https://x.com/jetbrains');
    });

    test('GitHub link opens JetBrains GitHub page', async ({ page, context }) => {
        const githubLink = page.getByTestId('organization-github-login');
        await expect(githubLink).toBeVisible();
        await githubLink.click();
        const newPagePromise = context.waitForEvent('page');
        const newPage = await newPagePromise;
        await newPage.waitForLoadState();
        await expect(newPage.url()).toContain('https://github.com/JetBrains');
    });

    test('Email link has the correct email address)', async ({ page }) => {
        const emailLink = page.getByTestId('organization-email-link');
        await expect(emailLink).toBeVisible();
        await expect(emailLink).toHaveAttribute('href', 'mailto:github@jetbrains.com');
    });

    test('The organization projects are visible', async ({ page }) => {
        const orgProjects = page.getByTestId('organization-projects');
        await expect(orgProjects).toBeVisible()
    });
});