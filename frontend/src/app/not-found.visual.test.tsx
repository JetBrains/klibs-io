import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

import NotFound from './not-found';

test('page not found', async () => {
    await page.viewport(1280, 900);
    await render(<NotFound />);

    await expect.element(page.getByTestId('not-found-page-message')).toHaveTextContent('Page not found');
    await expect.element(page.getByAltText('Kodee lost')).toBeVisible();
});
