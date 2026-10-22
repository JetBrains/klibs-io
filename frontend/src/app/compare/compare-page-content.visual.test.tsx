import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { test } from 'vitest';

import { comparedProjects } from '@/test/fixtures';
import ComparePageContent from './compare-page-content';

test('compare page with two projects', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={comparedProjects()} />);
});

test('compare page with nothing to compare', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={[]} />);
});

test('compare page when the comparison fails', async () => {
    await page.viewport(1280, 800);
    await render(<ComparePageContent projects={null} />);
});
