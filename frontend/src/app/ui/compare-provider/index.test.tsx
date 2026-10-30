import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';

import { projectSearchResult } from '@/test/fixtures';
import ProjectCard from '@/app/ui/project-card';
import CompareProvider from './index';

const push = vi.fn();

vi.mock('next/navigation', () => ({
    useRouter: () => ({ push }),
    usePathname: () => '/',
}));

function renderCards() {
    render(
        <CompareProvider>
            <ProjectCard featuredProject={projectSearchResult({ id: 1, name: 'Arrow' })} />
            <ProjectCard featuredProject={projectSearchResult({ id: 2, ownerLogin: 'InsertKoinIO', name: 'Koin' })} />
        </CompareProvider>,
    );
}

describe('CompareProvider', () => {
    test('shows the compare bar once a library is added to the comparison', async () => {
        renderCards();
        expect(screen.queryByRole('button', { name: /^Compare \(/ })).not.toBeInTheDocument();

        await userEvent.click(screen.getAllByText('Compare')[0]);

        expect(screen.getByRole('button', { name: 'Compare (1/4)' })).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Compare (1)' })).toBeChecked();
    });

    test('hides the compare bar when the comparison is cleared', async () => {
        renderCards();
        const [arrow, koin] = screen.getAllByText('Compare');
        await userEvent.click(arrow);
        await userEvent.click(koin);

        await userEvent.click(screen.getByRole('button', { name: 'Clear all' }));

        expect(screen.queryByRole('button', { name: /^Compare \(/ })).not.toBeInTheDocument();
        expect(screen.getAllByRole('checkbox', { name: 'Compare' })).toHaveLength(2);
    });

    test('opens the comparison page with the selected libraries', async () => {
        renderCards();
        const [arrow, koin] = screen.getAllByText('Compare');
        await userEvent.click(arrow);
        await userEvent.click(koin);

        await userEvent.click(screen.getByRole('button', { name: 'Compare (2/4)' }));

        expect(push).toHaveBeenCalledWith('/compare?projects=arrow-kt%2FArrow%2CInsertKoinIO%2FKoin');
    });
});
