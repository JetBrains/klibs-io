import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import SearchField from './index';

test('search field with a selected category', async () => {
    await render(
        <div style={{ width: 720 }}>
            <SearchField projectsCount="3200" selectedCategory="Featured" />
        </div>,
    );
});
