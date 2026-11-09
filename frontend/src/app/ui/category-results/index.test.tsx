import { act, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import { searchProjects } from '@/app/api';
import { Category, ProjectSearchResults } from '@/app/types';
import { projectSearchResult } from '@/test/fixtures';
import CategoryResults from './index';

vi.mock('@/app/api', () => ({
    searchProjects: vi.fn(),
}));

vi.mock('@/app/ui/project-card', () => ({
    default: ({ featuredProject }: { featuredProject: ProjectSearchResults }) => (
        <div>{featuredProject.name}</div>
    ),
}));

class MockIntersectionObserver {
    static instances: MockIntersectionObserver[] = [];

    constructor(private readonly callback: IntersectionObserverCallback) {
        MockIntersectionObserver.instances.push(this);
    }

    observe() {}

    unobserve() {}

    disconnect() {}

    takeRecords(): IntersectionObserverEntry[] {
        return [];
    }

    trigger() {
        this.callback(
            [{ isIntersecting: true } as IntersectionObserverEntry],
            this as unknown as IntersectionObserver,
        );
    }
}

const category: Category = {
    name: 'JetBrains and Google',
    subtitle: 'Essential libraries from the JetBrains and Google ecosystem',
    markers: ['JetBrains and Google'],
};

describe('CategoryResults', () => {
    beforeEach(() => {
        vi.mocked(searchProjects).mockReset();
        MockIntersectionObserver.instances = [];
        vi.stubGlobal('IntersectionObserver', MockIntersectionObserver);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    test('sends the special marker and sorts by dependents', async () => {
        vi.mocked(searchProjects).mockResolvedValue([projectSearchResult({ ownerLogin: 'Kotlin' })]);

        render(<CategoryResults category={category} searchQuery="compose" />);

        await waitFor(() => expect(searchProjects).toHaveBeenCalledWith({
            page: 1,
            limit: 15,
            markers: ['JetBrains and Google'],
            sort: 'most-dependents',
            query: 'compose',
        }));
        await screen.findByText('Arrow');
        expect(screen.getByRole('heading', { name: 'JetBrains and Google' })).toBeInTheDocument();
        expect(screen.getByText('Essential libraries from the JetBrains and Google ecosystem')).toBeInTheDocument();
    });

    test('sends the same filters and query on subsequent pages', async () => {
        const firstPage = Array.from({ length: 15 }, (_, index) =>
            projectSearchResult({ id: index + 1, name: `Project ${index + 1}` }));
        const secondPage = [projectSearchResult({ id: 16, name: 'Project 16' })];
        vi.mocked(searchProjects)
            .mockResolvedValueOnce(firstPage)
            .mockResolvedValueOnce(secondPage);

        render(<CategoryResults category={category} searchQuery="compose" />);

        await waitFor(() => expect(screen.getByText('Project 15')).toBeInTheDocument());
        await waitFor(() => expect(MockIntersectionObserver.instances.length).toBeGreaterThan(0));

        await act(async () => {
            MockIntersectionObserver.instances.at(-1)?.trigger();
        });

        await waitFor(() => expect(searchProjects).toHaveBeenNthCalledWith(2, {
            page: 2,
            limit: 15,
            markers: ['JetBrains and Google'],
            sort: 'most-dependents',
            query: 'compose',
        }));
        expect(await screen.findByText('Project 16')).toBeInTheDocument();
    });

    test('sends other marker filters without dependents sorting', async () => {
        vi.mocked(searchProjects).mockResolvedValue([]);
        render(<CategoryResults category={{ ...category, markers: ['FEATURED', 'GRANT_WINNER_2024'] }} />);

        await waitFor(() => expect(searchProjects).toHaveBeenCalledWith({
            page: 1,
            limit: 15,
            markers: ['FEATURED', 'GRANT_WINNER_2024'],
            sort: undefined,
            query: undefined,
        }));
    });

    test('sorts by dependents when the special marker is among other markers', async () => {
        vi.mocked(searchProjects).mockResolvedValue([]);
        render(<CategoryResults category={{ ...category, name: 'Another category', markers: ['FEATURED', 'JetBrains and Google'] }} />);

        await waitFor(() => expect(searchProjects).toHaveBeenCalledWith({
            page: 1,
            limit: 15,
            markers: ['FEATURED', 'JetBrains and Google'],
            sort: 'most-dependents',
            query: undefined,
        }));
    });

    test('shows no results without searching when category filters are empty', async () => {
        render(<CategoryResults category={{ ...category, markers: [] }} />);

        expect(await screen.findByText('No projects found in this category')).toBeInTheDocument();
        expect(searchProjects).not.toHaveBeenCalled();
    });

    test('clears results and ignores pending responses when filters become empty', async () => {
        let resolveSearch!: (results: ProjectSearchResults[]) => void;
        vi.mocked(searchProjects).mockReturnValue(new Promise(resolve => { resolveSearch = resolve; }));
        const { rerender } = render(<CategoryResults category={category} />);
        await waitFor(() => expect(searchProjects).toHaveBeenCalledTimes(1));

        rerender(<CategoryResults category={{ ...category, markers: [] }} />);
        expect(await screen.findByText('No projects found in this category')).toBeInTheDocument();
        await act(async () => resolveSearch([projectSearchResult()]));

        expect(screen.getByText('No projects found in this category')).toBeInTheDocument();
        expect(searchProjects).toHaveBeenCalledTimes(1);
    });

    test('refreshes results when category filters change', async () => {
        vi.mocked(searchProjects).mockResolvedValue([]);
        const { rerender } = render(<CategoryResults category={category} />);
        await waitFor(() => expect(searchProjects).toHaveBeenCalledTimes(1));

        rerender(<CategoryResults category={{ ...category, markers: ['FEATURED'] }} />);

        await waitFor(() => expect(searchProjects).toHaveBeenLastCalledWith({
            page: 1,
            limit: 15,
            markers: ['FEATURED'],
            sort: undefined,
            query: undefined,
        }));
    });
});
