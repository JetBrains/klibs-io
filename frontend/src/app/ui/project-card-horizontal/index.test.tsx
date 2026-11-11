import { render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';

import { projectSearchResult } from '@/test/fixtures';
import ProjectCardHorizontal from './index';

vi.mock('@rescui/icons', () => ({
    WinIcon: () => null,
}));

vi.mock('@/app/analytics', () => ({
    GAEvent: { PROJECT_CARD_CLICK: 'project-card-click' },
    trackEvent: vi.fn(),
}));

describe('ProjectCardHorizontal owner affiliation', () => {
    test.each([
        { ownerLogin: 'Kotlin', affiliation: 'jetbrains-kotlin', className: 'jetbrainsProject' },
        { ownerLogin: 'JetBrains', affiliation: 'jetbrains-kotlin', className: 'jetbrainsProject' },
        { ownerLogin: 'androidx', affiliation: 'androidx', className: 'androidxProject' },
    ])('highlights $ownerLogin projects', ({ ownerLogin, affiliation, className }) => {
        render(<ProjectCardHorizontal project={projectSearchResult({ ownerLogin, targetGroups: {} })} />);

        const card = screen.getByRole('link');
        expect(card).toHaveAttribute('data-project-affiliation', affiliation);
        expect(card.className).toMatch(new RegExp(className));
    });

    test('does not infer an affiliation for other owners', () => {
        render(<ProjectCardHorizontal project={projectSearchResult({ ownerLogin: 'Google', targetGroups: {} })} />);

        const card = screen.getByRole('link');
        expect(card).not.toHaveAttribute('data-project-affiliation');
        expect(card.className).not.toMatch(/jetbrainsProject|androidxProject/);
    });

    test.each([
        { ownerLogin: 'Kotlin', affiliation: 'jetbrains-kotlin', className: 'jetbrainsProject' },
        { ownerLogin: 'androidx', affiliation: 'androidx', className: 'androidxProject' },
    ])('preserves grant-winner styling for affiliated $ownerLogin projects', ({ ownerLogin, affiliation, className }) => {
        render(<ProjectCardHorizontal project={projectSearchResult({
            ownerLogin,
            markers: ['GRANT_WINNER_2024'],
            targetGroups: {},
        })} />);

        const card = screen.getByRole('link');
        expect(screen.getByText('Kotlin grant winner')).toBeInTheDocument();
        expect(card).toHaveAttribute('data-project-affiliation', affiliation);
        expect(card.className).toMatch(/grantProject/);
        expect(card.className).not.toMatch(new RegExp(className));
    });
});