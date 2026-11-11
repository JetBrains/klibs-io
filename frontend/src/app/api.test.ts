import { afterEach, expect, test, vi } from 'vitest';

import { searchProjects } from '@/app/api';

afterEach(() => {
    vi.unstubAllGlobals();
});

test('sends category filters in project search requests', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        void input;
        void init;
        return { json: async () => [] } as Response;
    });
    vi.stubGlobal('fetch', fetchMock);

    await searchProjects({
        page: 2,
        limit: 15,
        markers: ['FEATURED'],
        sort: 'most-dependents',
    });

    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, request] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/search/projects?page=2&limit=15');
    expect(JSON.parse(String(request?.body))).toEqual({
        sortBy: 'most-dependents',
        tags: [],
        markers: ['FEATURED'],
        targetGroupFilters: [],
    });
});
