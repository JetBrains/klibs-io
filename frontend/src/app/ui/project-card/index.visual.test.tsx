import '@/app/globals.css';
import 'bootstrap/dist/css/bootstrap.css';
import '@rescui/typography/lib/font-jb-sans-auto.css';

import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { test, vi } from 'vitest';

import { projectSearchResult } from '@/test/fixtures';
import ProjectCard from './index';

vi.mock('next/link', () => ({
    default: ({ children, ...props }: React.ComponentPropsWithoutRef<'a'>) => <a {...props}>{children}</a>,
}));

test('project card with project details', async () => {
    disableAutoSnapshot();

    await render(
        <div style={{ width: 380 }}>
            <ProjectCard
                featuredProject={projectSearchResult({
                    targetGroups: {
                        AndroidJvm: [],
                        JVM: ['11', '17'],
                        IOS: [],
                        JavaScript: [],
                    },
                    tags: ['functional-programming', 'typed-errors'],
                })}
            />
        </div>,
    );

    await takeSnapshot('project card');
});
