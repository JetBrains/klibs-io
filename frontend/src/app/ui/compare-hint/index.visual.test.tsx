import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import CompareHint from './index';

test('compare hint for a first-time visitor', async () => {
    localStorage.clear();
    await render(<CompareHint/>);
});
