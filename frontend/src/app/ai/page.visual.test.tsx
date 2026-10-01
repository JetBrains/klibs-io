import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

import AiPage from './page';

test('ai development page', async () => {
    await page.viewport(1280, 1600);
    await render(<AiPage />);

    await expect.element(page.getByRole('heading', { name: 'AI development with klibs.io', level: 1 })).toBeVisible();
    await expect.element(page.getByRole('heading', { name: 'AGENTS.md recommendation' })).toBeVisible();
    // The AGENTS.md snippet is highlighted asynchronously; an unhighlighted frame is a different image.
    await expect.poll(() => document.querySelectorAll('pre.shiki').length, { timeout: 10_000 }).toBeGreaterThan(0);
});
