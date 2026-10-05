import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { beforeEach, expect, test, vi } from 'vitest';

import { featuredProjectDetails, projectPackages, projectReadme } from '@/test/fixtures';
import { setNavigation } from '@/test/next-navigation';
import { waitForLayoutToSettle } from '@/test/visual';
import Project from './project-page-content';

beforeEach(() => {
    setNavigation({ pathname: '/project/arrow-kt/arrow' });
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(Date.UTC(2025, 2, 1));
});

const renderProject = (overrides: Parameters<typeof featuredProjectDetails>[0] = {}) => render(
    <Project
        initialProject={featuredProjectDetails(overrides)}
        initialPackages={projectPackages()}
        initialReadme={projectReadme()}
        projectName="arrow"
    />,
);

test('archived project page with packages and readme', async () => {
    await page.viewport(1280, 1400);
    await renderProject({ archived: true, archivedAtMillis: Date.UTC(2024, 10, 5) });

    await expect.element(page.getByText("The project's repository was archived on Nov 5, 2024.", { exact: false }))
        .toBeVisible();
    await expect.element(page.getByRole('heading', { name: 'Arrow', level: 1 }).first()).toBeVisible();
    await expect.element(page.getByTestId('readme-tab').first()).toHaveTextContent('typed functional programming');
});

test('project page packages tab', async () => {
    await page.viewport(1280, 1100);
    await renderProject();

    await page.getByRole('tab', { name: 'Packages' }).first().click();
    await expect.element(page.getByText('io.arrow-kt:arrow-fx-coroutines', { exact: false }).first()).toBeVisible();
    await waitForLayoutToSettle();
    await expect.element(page.getByText("The project's repository was archived", { exact: false }))
        .not.toBeInTheDocument();
});
