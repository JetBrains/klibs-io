import { test, expect } from '@playwright/test';
import { MainPage } from './pages/MainPage';

test.describe('Main page', () => {
    let mainPage: MainPage;

    test.beforeEach(async ({ page }) => {
        mainPage = new MainPage(page);
        await mainPage.open();
    });

    test('Click on the "Klibs.io" logo opens klibs.io', async ({ page }) => {
        await expect(mainPage.logoLink).toBeVisible();
        await mainPage.clickLogo();
        await expect(page.url()).toContain('/');
    });

    // Without click on the button, because the GitHub authorization is required.
    test('Click on "Report an issue" opens the related GitHub page', async () => {
        await expect(mainPage.reportIssueLink).toBeVisible();
        await mainPage.clickReportIssue();
        const href = await mainPage.getReportIssueHref();
        expect(href).toBe('https://github.com/JetBrains/klibs-io-issue-management/issues/new/choose');
    });

    test('Click on "FAQ" button opens the related page', async ({ page }) => {
        await expect(mainPage.faqLink).toBeVisible();
        await mainPage.clickFAQ();
        await page.waitForTimeout(2000);
        await expect(page.url()).toContain('/faq');
    });

    test('Click on "Slack" button opens the related page', async ({ page }) => {
        await expect(mainPage.slackLink).toBeVisible();
        await mainPage.clickSlack();
        await page.waitForTimeout(2000);
        await expect(page.url()).toContain('/faq#slack-guide');
    });

    test('Click on "Menu" button opens the related page', async () => {
        await expect(mainPage.ecosystemMenuButton).toBeVisible();
        await mainPage.openEcosystemMenu();
        await expect(mainPage.kotlinEcosystemPopupItem).toBeVisible();
    });

    test('Categories render correctly', async ({ page }) => {
        const firstCategory = page.locator('[data-testid^="category-section-"]').first();
        await expect(firstCategory).toBeVisible({ timeout: 10000 });
        await expect(firstCategory.locator('h2')).toBeVisible();
        await expect(firstCategory.locator('a[href^="/project/"]').first()).toBeVisible();
    });

    test('See All button navigates to category page', async ({ page }) => {
        const seeAllButton = page.locator('[data-testid^="see-all-button-"]').first();
        await expect(seeAllButton).toBeVisible({ timeout: 10000 });

        // Extract category slug before clicking to avoid race condition
        const categorySlug = await seeAllButton.evaluate(
            el => el.getAttribute('data-testid')?.replace('see-all-button-', '')
        );
        await seeAllButton.click();

        await expect(page).toHaveURL(new RegExp(`\\?category=${categorySlug}`));
    });

    test('Top tags expand and collapse correctly', async ({ page }) => {
        const showMoreButton = page.getByTestId('top-tags-show-more-button');
        await expect(showMoreButton).toBeVisible({ timeout: 10000 });

        const tags = page.getByRole('tab');
        const collapsedCount = await tags.count();

        // Expand - more tags should appear
        await showMoreButton.click();
        await expect(tags).toHaveCount(await tags.count());
        const expandedCount = await tags.count();
        expect(expandedCount).toBeGreaterThan(collapsedCount);

        // Collapse - tags count should return to original
        await showMoreButton.click();
        await expect(tags).toHaveCount(collapsedCount);
    });

    test('Grant winner banner Discover button navigates to grant-winners category', async ({ page }) => {
        const grantWinnerBanner = page.getByTestId('grant-winner-banner');
        await expect(grantWinnerBanner).toBeVisible({ timeout: 15000 });

        const discoverButton = page.getByTestId('grant-winners-discover-button');
        await discoverButton.click();

        await expect(page).toHaveURL(/\?category=grant-winners/);
    });

    test('Click on "Submit your project" button on the Kotlin Grant Winner banner opens the Kotlin Foundation grants page', async ({ page }) => {
        const grantWinnerBanner = page.getByTestId('grant-winner-banner');
        await expect(grantWinnerBanner).toBeVisible({ timeout: 15000 });

        const submitButton = grantWinnerBanner.getByRole('link', { name: 'Learn about grants' });
        await expect(submitButton).toBeVisible();
        await submitButton.click();
        await page.waitForTimeout(2000);
        await expect(page).toHaveURL('https://kotlinfoundation.org/grants/');
    });

    test('Click on "Create and submit your project" button opens the related FAQ section', async ({ page }) => {
        await expect(mainPage.createAndSubmitProjectLink.first()).toBeVisible();
        await mainPage.clickCreateAndSubmitProject();
        await expect(page).toHaveURL('/faq#how-do-i-add-a-project');
    });

    test('If there are less than 6 projects in category, there is the "Create and submit your own project" button', async ({ page }) => {
        const categories = page.locator('[data-testid^="category-section-"]');
        await expect(categories.first()).toBeVisible({ timeout: 10000 });

        for (const section of await categories.all()) {
            const projectsCount = await section.locator('a[href^="/project/"]').count();
            const placeholder = section.getByTestId('create-and-submit-project-link');

            if (projectsCount < 6) {
                // If there are fewer than 6 projects, the "Create and submit your own project" link should be visible
                await expect(placeholder).toBeVisible();
            } else {
                // If there are 6 or more projects, the link should not be present
                await expect(placeholder).toHaveCount(0);
            }
        }
    });

    test('If there are less than 6 projects in category, there is no "See all" button for this category', async ({ page }) => {
        const categories = page.locator('[data-testid^="category-section-"]');
        await expect(categories.first()).toBeVisible({ timeout: 10000 });

        for (const section of await categories.all()) {
            const projectsCount = await section.locator('a[href^="/project/"]').count();
            const seeAllButton = section.locator('[data-testid^="see-all-button-"]');

            if (projectsCount < 6) {
                // If there are fewer than 6 projects, the "See all" button should not be present
                await expect(seeAllButton).toHaveCount(0);
            }
        }
    });
});
