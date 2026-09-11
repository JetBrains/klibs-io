import '@/app/globals.css';
import 'bootstrap/dist/css/bootstrap.css';
import '@rescui/typography/lib/font-jb-sans-auto.css';

import { disableAutoSnapshot, takeSnapshot } from '@uiverify/vitest';
import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

import { SearchParams } from '@/app/types';
import SearchFilter from './index';

// Above the 1001px breakpoint, where the inline platform dropdown replaces the mobile trigger.
const DESKTOP = { width: 1280, height: 900 };

const filters: SearchParams = {
    mode: 'projects',
    page: 1,
    platforms: ['ios', 'jvm'],
    query: '',
    tags: [],
};

// The dropdown fades in; a mid-transition frame would archive as a spurious diff.
const disableAnimations = '*, *::before, *::after { animation: none !important; transition: none !important; }';

const renderFilter = () => render(
    <>
        <style>{disableAnimations}</style>
        <SearchFilter
            filters={filters}
            setFilters={() => {}}
            updateURLFromState={() => {}}
            projectsCount="3200"
        />
    </>,
);

test('search filter on desktop', async () => {
    disableAutoSnapshot();
    await page.viewport(DESKTOP.width, DESKTOP.height);

    await renderFilter();
    await takeSnapshot('desktop');

    await page.getByTestId('platform-filter-dropdown').click();
    // The sidebar holds a second set of checkboxes, and it precedes the dropdown in the DOM.
    await expect.element(page.getByRole('checkbox', { name: 'iOS' }).last()).toBeVisible();

    await takeSnapshot('desktop with the platform dropdown open');
});
