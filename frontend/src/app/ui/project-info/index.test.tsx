import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { projectDetails } from '@/test/fixtures';
import { ProjectInfo } from './index';

vi.mock('@rescui/icons', () => ({
    InfoOutlineIcon: () => null,
}));

vi.mock('@rescui/tooltip', () => ({
    Tooltip: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock('@rescui/typography', () => ({
    textCn: () => '',
}));

vi.mock('@/app/ui/time-ago', () => ({
    default: ({ timestamp }: { timestamp: number }) => <>{timestamp}</>,
}));

vi.mock('@/app/ui/featured-label', () => ({
    default: ({ isFeaturedProject, isGrantWinner }: {
        isFeaturedProject?: boolean;
        isGrantWinner?: boolean;
    }) => <span>{isGrantWinner ? 'Kotlin grant winner' : isFeaturedProject ? 'Featured project' : ''}</span>,
}));

vi.mock('@/app/analytics', () => ({
    GAEvent: { PROJECT_INFO_LINK_CLICK: 'project-info-link-click' },
    trackEvent: vi.fn(),
}));

describe('ProjectInfo', () => {
    test('builds the owner link and renders classification labels', () => {
        render(<ProjectInfo projectOverview={projectDetails({ markers: ['FEATURED', 'GRANT_WINNER_2025'] })} />);

        expect(screen.getByRole('link', { name: 'arrow-kt' }))
            .toHaveAttribute('href', '/organization/arrow-kt');
        expect(screen.getByText('Kotlin grant winner')).toBeInTheDocument();
        expect(screen.getByText('Featured project')).toBeInTheDocument();
    });
});
