import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { organization } from '@/test/fixtures';
import Organization from './organization-page-content';

vi.mock('next/image', () => ({
    default: ({ alt }: { alt: string }) => <span aria-label={alt} />,
}));

vi.mock('@/app/ui/container', () => ({
    default: ({ children, dataTestId }: { children: ReactNode; dataTestId?: string }) => (
        <div data-testid={dataTestId}>{children}</div>
    ),
}));

vi.mock('@/app/ui/project-card', () => ({
    default: () => null,
}));

describe('Organization page content', () => {
    test('builds the organization contact links', () => {
        render(
            <Organization
                initialOrganization={organization()}
                initialProjects={[]}
            />,
        );

        expect(screen.getAllByRole('link', { name: 'jetbrains' }).map(link => link.getAttribute('href')))
            .toEqual(['https://x.com/jetbrains', 'https://github.com/jetbrains']);
        expect(screen.getByRole('link', { name: 'github@jetbrains.com' }))
            .toHaveAttribute('href', 'mailto:github@jetbrains.com');
    });

    test('explains when the organization has no projects', () => {
        render(<Organization initialOrganization={organization()} initialProjects={[]} />);

        expect(screen.getByText('No projects')).toBeInTheDocument();
    });
});
