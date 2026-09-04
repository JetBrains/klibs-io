import '@/app/globals.css';
import 'bootstrap/dist/css/bootstrap.css';
import '@rescui/typography/lib/font-jb-sans-auto.css';

import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { test, vi } from 'vitest';

import { packageSearchResult } from '@/test/fixtures';
import PackageCard from './index';

vi.mock('next/link', () => ({
    default: ({ children, ...props }: React.ComponentPropsWithoutRef<'a'>) => <a {...props}>{children}</a>,
}));

test('package card with a highlighted search match', async () => {
    disableAutoSnapshot();

    await render(
        <div style={{ width: 380 }}>
            <PackageCard
                featuredPackage={packageSearchResult({
                    targetGroups: {
                        AndroidJvm: [],
                        JVM: ['11', '17'],
                        IOS: [],
                        JavaScript: [],
                    },
                })}
                search="arrow"
            />
        </div>,
    );

    await takeSnapshot('package card');
});
