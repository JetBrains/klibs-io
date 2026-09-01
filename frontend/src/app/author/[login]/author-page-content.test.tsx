import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { author } from '@/test/fixtures';
import Author from './author-page-content';

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

describe('Author page content', () => {
    test('builds the author social links', () => {
        render(<Author initialAuthor={author()} initialProjects={[]} />);

        expect(screen.getByRole('link', { name: 'alice_kotlin' }))
            .toHaveAttribute('href', 'https://x.com/alice_kotlin');
        expect(screen.getByRole('link', { name: 'alice' }))
            .toHaveAttribute('href', 'https://github.com/alice');
    });

    test('omits unavailable optional fields and explains an empty project list', () => {
        render(
            <Author
                initialAuthor={author({ company: '', description: null, homepage: '', location: '', twitterHandle: '' })}
                initialProjects={[]}
            />,
        );

        expect(screen.queryByText('Kotlin library author')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'alice_kotlin' })).not.toBeInTheDocument();
        expect(screen.getByText('No projects')).toBeInTheDocument();
    });
});
