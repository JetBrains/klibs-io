import { render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';

import { projectSearchResult } from '@/test/fixtures';
import ProjectCard from './index';

vi.mock('@rescui/icons', () => ({
    ReadIcon: () => null,
    RocketIcon: () => null,
    StarIcon: () => null,
    WinIcon: () => null,
}));

vi.mock('@/app/analytics', () => ({
    GAEvent: { PROJECT_CARD_CLICK: 'project-card-click' },
    trackEvent: vi.fn(),
}));

describe('ProjectCard owner affiliation', () => {
    test.each([
        { ownerLogin: 'Kotlin' },
        { ownerLogin: 'JetBrains' },
    ])('keeps the blue card styling without a Kotlin logo for $ownerLogin', ({ ownerLogin }) => {
        render(<ProjectCard featuredProject={projectSearchResult({ ownerLogin, targetGroups: {} })} />);

        const card = screen.getByRole('link');
        expect(screen.queryByLabelText('Kotlin logo')).not.toBeInTheDocument();
        expect(screen.queryByText('JetBrains / Kotlin')).not.toBeInTheDocument();
        expect(screen.queryByText('AndroidX')).not.toBeInTheDocument();
        expect(card).toHaveAttribute('data-project-affiliation', 'jetbrains-kotlin');
        expect(card.className).toMatch(/jetbrainsProject/);
    });

    test('adds green highlighting without Android branding to AndroidX cards', () => {
        render(<ProjectCard featuredProject={projectSearchResult({ ownerLogin: 'androidx', targetGroups: {} })} />);

        const card = screen.getByRole('link');
        expect(screen.queryByLabelText('Android robot logo')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Kotlin logo')).not.toBeInTheDocument();
        expect(card).toHaveAttribute('data-project-affiliation', 'androidx');
        expect(card.className).toMatch(/androidxProject/);
    });

    test('keeps grant-winner labeling without the Kotlin logo', () => {
        render(<ProjectCard featuredProject={projectSearchResult({
            ownerLogin: 'Kotlin',
            markers: ['GRANT_WINNER_2024'],
            targetGroups: {},
        })} />);

        expect(screen.getByText('Kotlin grant winner')).toBeInTheDocument();
        expect(screen.queryByText('JetBrains / Kotlin')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Kotlin logo')).not.toBeInTheDocument();
        expect(screen.getByRole('link')).toHaveAttribute('data-project-affiliation', 'jetbrains-kotlin');
    });

    test('does not infer an affiliation from another owner', () => {
        render(<ProjectCard featuredProject={projectSearchResult({ ownerLogin: 'Google', targetGroups: {} })} />);

        expect(screen.queryByLabelText('Kotlin logo')).not.toBeInTheDocument();
        expect(screen.queryByText('JetBrains / Kotlin')).not.toBeInTheDocument();
        expect(screen.queryByText('AndroidX')).not.toBeInTheDocument();
        expect(screen.getByRole('link')).not.toHaveAttribute('data-project-affiliation');
    });
});
