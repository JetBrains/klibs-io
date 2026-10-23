import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import CompareBar from './index';

test('compare bar with selected libraries', async () => {
    await render(
        <div style={{ width: 1024 }}>
            <CompareBar
                items={[
                    { ownerLogin: 'JetBrains', name: 'compose-multiplatform' },
                    { ownerLogin: 'InsertKoinIO', name: 'koin' },
                    { ownerLogin: 'ktorio', name: 'ktor' },
                ]}
                maxItems={4}
                onRemove={() => {}}
                onClear={() => {}}
                onCompare={() => {}}
            />
        </div>,
    );
});
