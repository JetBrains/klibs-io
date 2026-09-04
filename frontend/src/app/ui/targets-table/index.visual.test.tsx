import '@/app/globals.css';
import 'bootstrap/dist/css/bootstrap.css';
import '@rescui/typography/lib/font-jb-sans-auto.css';

import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import { packageOverview } from '@/test/fixtures';
import TargetsTable from './index';

// Spans every platform color and gives Kotlin/Native two groups, so the row radii show up too.
test('targets table across all platforms', async () => {
    disableAutoSnapshot();

    await render(
        <div style={{ width: 420 }}>
            <TargetsTable
                projectPackage={packageOverview({
                    targetGroups: {
                        AndroidJvm: ['11'],
                        JVM: ['11', '17'],
                        IOS: ['iosArm64', 'iosSimulatorArm64', 'iosX64'],
                        MacOS: ['macosArm64', 'macosX64'],
                        Wasm: ['wasmJs'],
                        JavaScript: ['js'],
                    },
                })}
            />
        </div>,
    );

    await takeSnapshot('targets table');
});
