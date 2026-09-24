import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

import PageLoader from './index';

// Every route's loading.tsx renders this full-screen loader.
test('route loading screen', async () => {
    await page.viewport(1280, 720);
    await render(<PageLoader />);

    await expect.element(page.getByAltText('Kodee spinning')).toBeVisible();
});
