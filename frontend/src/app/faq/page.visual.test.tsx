import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

import Faq from './page';

test('faq page', async () => {
    await page.viewport(1280, 900);
    await render(<Faq />);

    await expect.element(page.getByRole('heading', { name: 'FAQ', level: 1 })).toBeVisible();
});
