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
    markers: [],
    selector: 'JETBRAINS_AND_GOOGLE',
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

    test('keeps the owner selector with empty markers when searching a category', async () => {
        vi.mocked(searchProjects).mockResolvedValue([projectSearchResult({ ownerLogin: 'Kotlin' })]);

        render(<CategoryResults category={category} searchQuery="compose" />);

        await waitFor(() => expect(searchProjects).toHaveBeenCalledWith({
            page: 1,
            limit: 15,
            markers: [],
            categorySelector: 'JETBRAINS_AND_GOOGLE',
            query: 'compose',
        }));
        expect(screen.getByRole('heading', { name: 'JetBrains and Google' })).toBeInTheDocument();
        expect(screen.getByText('Essential libraries from the JetBrains and Google ecosystem')).toBeInTheDocument();
    });

    test('sends the same owner selector and query on subsequent pages', async () => {
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
            markers: [],
            categorySelector: 'JETBRAINS_AND_GOOGLE',
            query: 'compose',
        }));
        expect(await screen.findByText('Project 16')).toBeInTheDocument();
    });
});
