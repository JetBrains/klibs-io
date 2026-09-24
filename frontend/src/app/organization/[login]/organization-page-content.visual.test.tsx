import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

import { bundledAvatarUrl, organization, organizationProjects } from '@/test/fixtures';
import Organization from './organization-page-content';

test('organization page with featured and grant-winner projects', async () => {
    await page.viewport(1280, 1100);
    await render(
        <Organization
            initialOrganization={organization({ avatarUrl: bundledAvatarUrl() })}
            initialProjects={organizationProjects()}
        />,
    );

    await expect.element(page.getByTestId('organization-name')).toHaveTextContent('JetBrains');
    await expect.element(page.getByText('Kotlin grant winner')).toBeVisible();
    await expect.element(page.getByText('Featured')).toBeVisible();
    await expect.element(page.getByRole('heading', { name: 'kotlinx.coroutines' })).toBeVisible();
});
