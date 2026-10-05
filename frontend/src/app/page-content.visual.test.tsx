import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { beforeEach, expect, test, vi } from 'vitest';

import { searchPackages, searchProjects } from '@/app/api';
import { homeCategories, networkingProjects, serializationPackages } from '@/test/fixtures';
import { setNavigation } from '@/test/next-navigation';
import { renderSingleAnimationFrame } from '@/test/visual';
import PageContent from './page-content';

vi.mock('@/app/api', () => import('@/test/api'));

const renderHome = async (search: string) => {
    const categoryWithProjects = homeCategories();
    setNavigation({ pathname: '/', search });
    await page.viewport(1280, 1300);
    await render(
        <PageContent
            categories={categoryWithProjects.map((c) => c.category)}
            categoryWithProjects={categoryWithProjects}
            projectsCount="3200"
        />,
    );
};

beforeEach(() => {
    renderSingleAnimationFrame();
});

test('home page categories view without search filters', async () => {
    await renderHome('');

    await expect.element(page.getByText('serialization', { exact: false }).first()).toBeVisible();
    await expect.element(page.getByTestId('category-section-networking')).toBeVisible();
    await expect.element(page.getByRole('heading', { name: 'Apollo Kotlin' })).toBeVisible();
    await expect.element(page.getByText('Featured')).toBeVisible();
    await expect.element(page.getByAltText('Kodee grant winner image')).toBeVisible();
});

test('home page project search results with the query highlighted', async () => {
    vi.mocked(searchProjects).mockResolvedValue(networkingProjects());
    await renderHome('query=ktor');

    await expect.element(page.getByRole('heading', { name: 'Apollo Kotlin' })).toBeVisible();
    await expect.poll(() => document.querySelectorAll('[class*="highlight"]').length).toBeGreaterThan(0);
    await expect.element(page.getByTestId('category-section-networking')).not.toBeInTheDocument();
});

test('home page search with no results', async () => {
    vi.mocked(searchProjects).mockResolvedValue([]);
    await renderHome('query=nothing-matches');

    await expect.element(page.getByTestId('search-no-results-message')).toBeVisible();
    await expect.element(page.getByAltText('Kodee 404')).toBeVisible();
});

test('home page package search results', async () => {
    vi.mocked(searchPackages).mockResolvedValue(serializationPackages());
    await renderHome('mode=packages&query=serialization');

    await expect.element(page.getByText('kotlinx-serialization-cbor', { exact: false }).first()).toBeVisible();
    await expect.element(page.getByText('kaml', { exact: false }).first()).toBeVisible();
});

test('home page category view', async () => {
    vi.mocked(searchProjects).mockResolvedValue(networkingProjects());
    await renderHome('category=networking');

    await expect.element(page.getByRole('heading', { name: 'Ktorfit' })).toBeVisible();
    await expect.element(page.getByTestId('category-clear-tag')).toHaveTextContent(/networking/i);
});
