import { act, fireEvent, render, screen } from '@testing-library/react';
import { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import SearchField from './index';

vi.mock('@rescui/icons', () => ({
    CloseIcon: () => null,
    SearchIcon: () => null,
}));

vi.mock('@rescui/tag', () => ({
    presets: { 'outline-dark': {} },
    Tag: ({ children }: { children: ReactNode }) => <span>{children}</span>,
}));

vi.mock('@/app/analytics', () => ({
    GAEvent: { SEARCH_KEYBOARD_TRIGGER: 'search-keyboard-trigger' },
    trackEvent: vi.fn(),
}));

describe('SearchField', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    test('reports the search value after the debounce delay', () => {
        const onChange = vi.fn();
        render(<SearchField onChange={onChange} projectsCount="100" />);

        fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Ktor' } });
        expect(onChange).not.toHaveBeenCalled();

        act(() => vi.advanceTimersByTime(200));

        expect(onChange).toHaveBeenCalledWith('Ktor');
    });

    test('clears the query with Escape and keeps the field focused', () => {
        const onClear = vi.fn();
        render(<SearchField value="Koin" onClear={onClear} />);
        const input = screen.getByRole('textbox');

        fireEvent.focus(input);
        fireEvent.keyDown(input, { key: 'Escape' });

        expect(input).toHaveValue('');
        expect(input).toHaveFocus();
        expect(onClear).toHaveBeenCalledOnce();
    });

    test('shows and clears the selected category', () => {
        const onCategoryReset = vi.fn();
        render(
            <SearchField
                selectedCategory="Featured"
                onCategoryReset={onCategoryReset}
            />,
        );

        expect(screen.getByRole('textbox')).toHaveAttribute('placeholder', 'Search in Featured');
        fireEvent.keyDown(screen.getByRole('button', { name: 'Clear Featured filter' }), { key: 'Enter' });

        expect(onCategoryReset).toHaveBeenCalledOnce();
    });

    test('focuses the search field with the slash shortcut', () => {
        render(<SearchField />);

        fireEvent.keyDown(window, { code: 'Slash' });

        expect(screen.getByRole('textbox')).toHaveFocus();
    });
});
