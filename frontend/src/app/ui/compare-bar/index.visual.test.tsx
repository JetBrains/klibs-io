import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import CompareBar from './index';

test('compare bar with selected libraries', async () => {
    await render(
        <div style={{ width: 1024 }}>
            <CompareBar
                items={[
                    { id: 1, ownerLogin: 'JetBrains', name: 'compose-multiplatform' },
                    { id: 2, ownerLogin: 'InsertKoinIO', name: 'koin' },
                    { id: 3, ownerLogin: 'ktorio', name: 'ktor' },
                ]}
                maxItems={10}
                onRemove={() => {}}
                onClear={() => {}}
                onCompare={() => {}}
            />
        </div>,
    );
});
