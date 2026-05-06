import { test, expect } from '@playwright/test';
import { EcosystemMenu } from './pages/EcosystemMenu';


test.describe('Kotlin Ecosystem menu', () => {
    let ecosystemMenu: EcosystemMenu;

    test.beforeEach(async ({page}) => {
        ecosystemMenu = new EcosystemMenu(page);
        await ecosystemMenu.open();
    });

    // Violet button
    test('Click on violet "Kotlin documentation" button opens the docs page', async ({ page }) => {
        await ecosystemMenu.clickMainLink('Kotlin documentation');
        await expect(page).toHaveURL('https://kotlinlang.org/docs/home.html');
    });

    // Violet button
    test('Click on violet "Kotlin playground" button opens the playground page', async ({ page }) => {
        await ecosystemMenu.clickMainLink('Kotlin playground');
        await expect(page).toHaveURL('https://play.kotlinlang.org/');
    });

    // KMP section
    test('Click on "KMP documentation" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('KMP documentation');
        await expect(page).toHaveURL('https://kotlinlang.org/docs/multiplatform/get-started.html');
    });

    // Added a timeout because without it the test fails. The page doesn't have time to open.
    test('Click on "KMP wizard" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('KMP wizard');
        await page.waitForTimeout(2000)
        await expect(page.url()).toContain('https://kmp.jetbrains.com/');

    });

    test('Click on "KMP libraries (klibs.io)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('KMP libraries (klibs.io)');
        await expect(page).toHaveURL('https://klibs.io/');
    });

    // Ktor section
    test('Click on "Ktor documentation" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Ktor documentation');
        await expect(page).toHaveURL('https://ktor.io/docs/welcome.html');
    });

    test('Click on "Ktor wizard" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Ktor wizard');
        await expect(page).toHaveURL('https://start.ktor.io/settings');
    });

    test('Click on "Ktor API reference" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Ktor API reference');
        await expect(page).toHaveURL('https://api.ktor.io/');
    });

    // API References section
    test('Click on "Standard library (stdlib)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Standard library (stdlib)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/core/kotlin-stdlib/');
    });

    test('Click on "Test library (kotlin.test)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Test library (kotlin.test)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/core/kotlin-test/');
    });

    test('Click on "Coroutines (kotlinx.coroutines)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Coroutines (kotlinx.coroutines)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/');
    });

    test('Click on "Serialization (kotlinx.serialization)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Serialization (kotlinx.serialization)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/');
    });

    test('Click on "Kotlin I/O library (kotlinx-io)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Kotlin I/O library (kotlinx-io)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/kotlinx-io/');
    });

    test('Click on "Data and time (kotlinx-datetime)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Data and time (kotlinx-datetime)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/kotlinx-datetime/');
    });

    test('Click on "JVM Metadata (kotlin-metadata-jvm)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('JVM Metadata (kotlin-metadata-jvm)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/kotlinx-metadata-jvm/');
    });

    test('Click on "Kotlin Gradle plugins (kotlin-gradle-plugin)" button opens the related page', async ({ page }) => {
        await ecosystemMenu.clickLink('Kotlin Gradle plugins (kotlin-gradle-plugin)');
        await expect(page).toHaveURL('https://kotlinlang.org/api/kotlin-gradle-plugin/');
    });
});