import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { test } from 'vitest';

import { cardTargetGroups, ecosystemProjects, projectSearchResult } from '@/test/fixtures';
import ProjectCard from './index';

test('project card with project details', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 900);

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

test('project cards with JetBrains/Kotlin and AndroidX color styling', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 900);

    await render(
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 380px)', gap: 16 }}>
            {ecosystemProjects().map(project => (
                <ProjectCard key={project.id} featuredProject={project} />
            ))}
        </div>,
    );

    await takeSnapshot('project card owner affiliations');
});
