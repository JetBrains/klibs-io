import '@rescui/typography/lib/font-jb-sans-auto.css';
import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import '@/app/globals.css';

import { render } from 'vitest-browser-react';
import { test, vi } from 'vitest';
import bootstrapIconsFont from 'bootstrap-icons/font/fonts/bootstrap-icons.woff2?inline';

import { author } from '@/test/fixtures';
import Author from './author-page-content';

vi.mock('@/app/ui/project-card', () => ({
    default: () => null,
}));

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
        <>
            <style>{`
                @font-face {
                    font-family: 'bootstrap-icons-inline';
                    src: url('${bootstrapIconsFont}') format('woff2');
                }
                .bi::before {
                    font-family: 'bootstrap-icons-inline' !important;
                }
            `}</style>
            <Author
                initialAuthor={author({ avatarUrl: 'deterministic-avatar' })}
                initialProjects={[]}
            />
        </>,
    );
});
