import { test, expect } from '@playwright/test';

test.describe('FAQ page links', () => {

    test.beforeEach(async ({ page }) => {
        await page.goto('/faq');
    });

    test('Link 1: Maven central public index', async ({ page }) => {
        const link = page.locator('a[href="https://repo1.maven.org/maven2/.index/"]');
        await expect(link).toBeVisible();
    });

    test('Link 2: Kotlin tooling metadata JSON', async ({ page }) => {
        const href = 'https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0-kotlin-tooling-metadata.json';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });

    test('Link 3: POM example', async ({ page }) => {
        const href = 'https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0.pom';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });

    test('Link 4: Submit an issue on GitHub', async ({ page }) => {
        const href = 'https://github.com/JetBrains/klibs-io-issue-management/issues/new?assignees=&labels=question&projects=&template=question.md&title=';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });

    test('Link 5: Multiplatform samples in the Kotlin docs', async ({ page }) => {
        const link = page.locator('a[href*="kotlinlang.org/docs/multiplatform/multiplatform-samples.html"]');
        await expect(link).toBeVisible();
        const href = await link.getAttribute('href');
        expect(href).toContain('https://kotlinlang.org/docs/multiplatform/multiplatform-samples.html');
    });

    test('Link 6: GitHub issue tracker', async ({ page }) => {
        const href = 'https://github.com/JetBrains/klibs-io/issues/new/choose';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });

    test('Link 7: klibs-io Slack channel', async ({ page }) => {
        const href = 'https://kotlinlang.slack.com/archives/C081AF4JK70';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });

    test('Link 8: Member application form', async ({ page }) => {
        const link = page.locator('a[href*="surveys.jetbrains.com/s3/kotlin-slack-sign-up"]');
        await expect(link).toBeVisible();
        const href = await link.getAttribute('href');
        expect(href).toContain('https://surveys.jetbrains.com/s3/kotlin-slack-sign-up');
    });

    test('Link 9: Ignat Beresnev Twitter', async ({ page }) => {
        const href = 'https://x.com/IgnatBeresnev';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });

    test('Link 10: YouTube video about Klibs', async ({ page }) => {
        const href = 'https://www.youtube.com/watch?v=rKbM3e0OidI';
        const link = page.locator(`a[href="${href}"]`);
        await expect(link).toBeVisible();
    });
});