import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import { expect, test, vi } from 'vitest';

import { author, authorProjects } from '@/test/fixtures';
import Author from './author-page-content';

vi.mock('next/image', () => ({
    default: ({ alt, className, height, width }: {
        alt: string;
        className?: string;
        height: number;
        width: number;
    }) => (
        <svg aria-label={alt} className={className} height={height} role="img" viewBox="0 0 200 200" width={width}>
            <rect fill="#7f52ff" height="200" width="200" />
            <circle cx="100" cy="76" fill="#ffffff" r="38" />
            <path d="M38 190c8-44 31-66 62-66s54 22 62 66" fill="#ffffff" />
        </svg>
    ),
}));

test('author profile with contact details', async () => {
    await render(
        <Author
            initialAuthor={author({ avatarUrl: 'deterministic-avatar' })}
            initialProjects={[]}
        />,
    );
});

test('author profile with projects', async () => {
    await page.viewport(1280, 1100);
    await render(
        <Author
            initialAuthor={author({ avatarUrl: 'deterministic-avatar' })}
            initialProjects={authorProjects()}
        />,
    );

    await expect.element(page.getByRole('heading', { name: 'kmp-settings' })).toBeVisible();
    await expect.element(page.getByText('No projects')).not.toBeInTheDocument();
});
