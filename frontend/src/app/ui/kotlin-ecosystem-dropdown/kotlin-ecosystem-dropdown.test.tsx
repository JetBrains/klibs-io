import { fireEvent, render, screen } from '@testing-library/react';
import { ComponentProps, forwardRef, ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { KotlinEcosystemDropdown } from './kotlin-ecosystem-dropdown';

vi.mock('@rescui/button', () => ({
    Button: forwardRef<HTMLButtonElement, ComponentProps<'button'> & { content?: string; icon?: ReactNode }>(
        function Button({ content, icon, ...props }, ref) {
            void content;
            return <button ref={ref} {...props}>{icon}</button>;
        },
    ),
}));

vi.mock('@rescui/icons', () => ({
    ArrowRightIcon: () => null,
    MoreIcon: () => <>Menu</>,
}));

vi.mock('@rescui/typography', () => ({
    textCn: () => '',
}));

vi.mock('next/image', () => ({
    default: ({ alt }: { alt: string }) => <span aria-label={alt} />,
}));

vi.mock('react-remove-scroll-bar', () => ({
    RemoveScrollBar: () => null,
}));

vi.mock('@/app/hooks/use-focus-trap', () => ({
    useFocusTrap: vi.fn(),
}));

vi.mock('@/app/analytics', () => ({
    GAEvent: { KOTLIN_ECOSYSTEM_DROPDOWN_CLICK: 'kotlin-ecosystem-dropdown-click' },
    trackEvent: vi.fn(),
}));

describe('KotlinEcosystemDropdown', () => {
    test('opens the navigation and closes it with Escape', () => {
        render(<KotlinEcosystemDropdown />);

        fireEvent.click(screen.getByRole('button', { name: 'Menu' }));

        expect(screen.getByRole('link', { name: /Kotlin documentation/ })).toBeVisible();
        expect(document.documentElement).toHaveClass('scroll-lock');

        fireEvent.keyDown(document, { key: 'Escape' });

        expect(screen.queryByRole('link', { name: /Kotlin documentation/ })).not.toBeInTheDocument();
        expect(document.documentElement).not.toHaveClass('scroll-lock');
    });
});
