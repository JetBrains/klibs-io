import { render, screen } from '@testing-library/react';
import { ComponentProps } from 'react';
import { describe, expect, test, vi } from 'vitest';

import GrantWinnerBanner from './index';

vi.mock('@rescui/button', () => ({
    Button: ({ children, href, ...props }: ComponentProps<'a'>) => (
        <a href={href} {...props}>{children}</a>
    ),
}));

vi.mock('@rescui/typography', () => ({
    textCn: () => '',
}));

vi.mock('next/image', () => ({
    default: ({ alt }: { alt: string }) => <span aria-label={alt} />,
}));

vi.mock('./background-squares', () => ({
    BackgroundSquares: () => null,
}));

describe('GrantWinnerBanner', () => {
    test('links to the supplied category', () => {
        render(<GrantWinnerBanner categorySlug="grant-winners" />);

        expect(screen.getByRole('link', { name: 'Discover' }))
            .toHaveAttribute('href', '/?category=grant-winners');
    });
});
