import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

import { comparedProjects } from '@/test/fixtures';
import { setNavigation } from '@/test/next-navigation';
import ComparePageContent from './compare-page-content';

// Relative dates and freshness depend on the current time
beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2024-01-01T00:00:00Z'));
    // The page only shows projects listed in the URL
    const references = comparedProjects().map(it => `projects=${it.ownerLogin}/${it.projectName}`);
    setNavigation({ pathname: '/compare', search: `?${references.join('&')}` });
});

afterEach(() => {
    vi.useRealTimers();
});

test('compare page with two projects', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={comparedProjects()} />);
});

test('compare page with all properties unselected', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={comparedProjects()} />);
    for (const tab of page.getByRole('tab').all()) {
        await tab.click();
    }
    await expect.element(page.getByText('No properties selected.', { exact: false })).toBeVisible();
});

test('compare page with nothing to compare', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={[]} />);
});

test('compare page when the comparison fails', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={null} />);
});
