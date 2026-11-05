import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { beforeEach, test, vi } from 'vitest';

import { cardTargetGroups, packageSearchResult } from '@/test/fixtures';
import PackageCardHorizontal from './index';

beforeEach(() => {
    // The latest release date is rendered relative to now
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(Date.UTC(2025, 2, 1));
});

test('horizontal package card with a highlighted search match', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 400);

    await render(
        <div style={{ width: 1240 }}>
            <PackageCardHorizontal
                pkg={packageSearchResult({ targetGroups: cardTargetGroups() })}
                search="arrow"
            />
        </div>,
    );

    await takeSnapshot('package card horizontal');
});
