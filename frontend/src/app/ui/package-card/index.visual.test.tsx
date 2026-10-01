import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import { cardTargetGroups, packageSearchResult } from '@/test/fixtures';
import PackageCard from './index';

test('package card with a highlighted search match', async () => {
    disableAutoSnapshot();

    await render(
        <div style={{ width: 380 }}>
            <PackageCard
                featuredPackage={packageSearchResult({ targetGroups: cardTargetGroups() })}
                search="arrow"
            />
        </div>,
    );

    await takeSnapshot('package card');
});
