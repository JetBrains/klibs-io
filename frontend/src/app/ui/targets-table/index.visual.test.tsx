import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import { allPlatformTargets, packageOverview } from '@/test/fixtures';
import TargetsTable from './index';

test('targets table across all platforms', async () => {
    disableAutoSnapshot();

    await render(
        <div style={{ width: 420 }}>
            <TargetsTable
                projectPackage={packageOverview({ targetGroups: allPlatformTargets() })}
            />
        </div>,
    );

    await takeSnapshot('targets table');
});
