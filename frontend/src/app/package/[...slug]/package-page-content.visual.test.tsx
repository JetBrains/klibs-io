import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { beforeEach, expect, test, vi } from 'vitest';

import { groupArtifacts, multiplatformTargets, packageDetails, packageReleases, projectDetails } from '@/test/fixtures';
import { setNavigation } from '@/test/next-navigation';
import Package from './package-page-content';

const renderPackagePage = () => render(
    <Package
        initialPackage={packageDetails({ targetGroups: multiplatformTargets() })}
        initialParentProject={projectDetails({ scmStars: 6_300 })}
        initialPackageVersions={packageReleases()}
        initialGroupArtifacts={groupArtifacts()}
        version="2.0.0"
    />,
);

beforeEach(() => {
    // The Gradle Kotlin/Groovy tab choice persists in localStorage across tests.
    localStorage.clear();
    setNavigation({ pathname: '/package/io.arrow-kt/arrow-core/2.0.0' });
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(Date.UTC(2025, 2, 1));
});

const expectLoadedPackage = async () => {
    await expect.element(page.getByRole('heading', { name: 'arrow-core:2.0.0' })).toBeVisible();
    await expect.element(page.getByRole('link', { name: 'Arrow (6.3k stars)' })).toBeVisible();
    await expect.element(page.getByText(/2\.0\.0 \(about 2 months ago\)/)).toBeVisible();
    await expect.element(page.getByText('implementation("io.arrow-kt:arrow-core:2.0.0")')).toBeVisible();
};

test('package page with version history on desktop', async () => {
    await page.viewport(1280, 1400);
    await renderPackagePage();
    await expectLoadedPackage();
    await expect.element(page.getByRole('link', { name: '1.1.5' }).first()).toBeVisible();
});

test('package page with version history on mobile', async () => {
    await page.viewport(390, 1800);
    await renderPackagePage();
    await expectLoadedPackage();
    // Below the medium breakpoint the version table switches to its stacked mobile layout.
    await expect.element(page.getByText('Release:').last()).toBeVisible();
});
