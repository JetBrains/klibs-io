import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test, vi } from 'vitest';

import { insightsLeaderboard } from '@/test/fixtures';
import Insights from './page';

const renderInsights = async (response: () => Promise<Response>) => {
    vi.stubGlobal('fetch', vi.fn(response));
    await page.viewport(1280, 800);
    await render(<Insights />);
};

test('insights leaderboard sorted by dependents', async () => {
    await renderInsights(async () => Response.json(insightsLeaderboard()));

    await expect.element(page.getByText('Kotlin/kotlinx.coroutines')).toBeVisible();
    await expect.element(page.getByText('Dependents ↓')).toBeVisible();
    await expect.element(page.getByAltText('Kodee spinning')).not.toBeInTheDocument();
});

test('insights when the search API fails', async () => {
    await renderInsights(async () => {
        throw new Error('Service unavailable');
    });

    await expect.element(page.getByRole('alert')).toHaveTextContent('Failed to load insights: Service unavailable');
});
