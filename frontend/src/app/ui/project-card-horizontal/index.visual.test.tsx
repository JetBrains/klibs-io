import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { beforeEach, test, vi } from 'vitest';

import { cardTargetGroups, projectSearchResult } from '@/test/fixtures';
import ProjectCardHorizontal from './index';

beforeEach(() => {
    // The latest release date is rendered relative to now
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(Date.UTC(2025, 2, 1));
});

test('horizontal project card with a highlighted search match', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 400);

    await render(
        <div style={{ width: 1240 }}>
            <ProjectCardHorizontal
                project={projectSearchResult({
                    targetGroups: cardTargetGroups(),
                    tags: ['functional-programming', 'typed-errors'],
                })}
                search="arrow"
            />
        </div>,
    );

    await takeSnapshot('project card horizontal');
});

test('horizontal project cards with JetBrains and Google ecosystem highlighting', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 400);

    await render(
        <div style={{ width: 1240, display: 'flex', gap: 16 }}>
            <ProjectCardHorizontal
                project={projectSearchResult({
                    name: 'compose-multiplatform',
                    ownerLogin: 'JetBrains',
                    targetGroups: cardTargetGroups(),
                })}
            />
            <ProjectCardHorizontal
                project={projectSearchResult({
                    name: 'Room',
                    ownerLogin: 'androidx',
                    targetGroups: cardTargetGroups(),
                })}
            />
        </div>,
    );

    await takeSnapshot('project cards horizontal ecosystem highlighting');
});

test('horizontal project card of a Kotlin grant winner', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 400);

    await render(
        <div style={{ width: 1240 }}>
            <ProjectCardHorizontal
                project={projectSearchResult({
                    targetGroups: cardTargetGroups(),
                    tags: ['functional-programming', 'typed-errors'],
                    markers: ['GRANT_WINNER_2024'],
                })}
            />
        </div>,
    );

    await takeSnapshot('project card horizontal grant winner');
});

test('horizontal project card of an affiliated grant winner', async () => {
    disableAutoSnapshot();
    await page.viewport(1280, 400);

    await render(
        <div style={{ width: 1240 }}>
            <ProjectCardHorizontal
                project={projectSearchResult({
                    name: 'kotlinx.coroutines',
                    ownerLogin: 'Kotlin',
                    targetGroups: cardTargetGroups(),
                    tags: ['functional-programming', 'typed-errors'],
                    markers: ['GRANT_WINNER_2024'],
                })}
            />
        </div>,
    );

    await takeSnapshot('project card horizontal affiliated grant winner');
});
