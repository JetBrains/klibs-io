import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test } from 'vitest';

import { projectSearchResult } from '@/test/fixtures';
import ProjectCard from '@/app/ui/project-card';
import CompareProvider from './index';

function renderCards() {
    render(
        <CompareProvider>
            <ProjectCard featuredProject={projectSearchResult({ id: 1, name: 'Arrow' })} />
            <ProjectCard featuredProject={projectSearchResult({ id: 2, name: 'Koin' })} />
        </CompareProvider>,
    );
}

describe('CompareProvider', () => {
    test('shows the compare bar once a library is added to the comparison', async () => {
        renderCards();
        expect(screen.queryByRole('button', { name: /^Compare \(/ })).not.toBeInTheDocument();

        await userEvent.click(screen.getAllByRole('checkbox', { name: 'Compare' })[0]);

        expect(screen.getByRole('button', { name: 'Compare (1/10)' })).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Compare (1)' })).toBeChecked();
    });

    test('hides the compare bar when the comparison is cleared', async () => {
        renderCards();
        const [arrow, koin] = screen.getAllByRole('checkbox', { name: 'Compare' });
        await userEvent.click(arrow);
        await userEvent.click(koin);

        await userEvent.click(screen.getByRole('button', { name: 'Clear' }));

        expect(screen.queryByRole('button', { name: /^Compare \(/ })).not.toBeInTheDocument();
        expect(screen.getAllByRole('checkbox', { name: 'Compare' })).toHaveLength(2);
    });
});
