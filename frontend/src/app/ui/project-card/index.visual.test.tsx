import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { test } from 'vitest';

import { cardTargetGroups, projectSearchResult } from '@/test/fixtures';
import ProjectCard from './index';

test('project card with project details', async () => {
    disableAutoSnapshot();

    await render(
        <div style={{ width: 380 }}>
            <ProjectCard
                featuredProject={projectSearchResult({
                    targetGroups: cardTargetGroups(),
                    tags: ['functional-programming', 'typed-errors'],
                })}
            />
        </div>,
    );

    await takeSnapshot('project card');
});
