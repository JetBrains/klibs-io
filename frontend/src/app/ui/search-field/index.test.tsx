import { fireEvent, render, screen } from '@testing-library/react';
import { ReactNode, useState } from 'react';
import { describe, expect, test, vi } from 'vitest';

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

const ControlledSearchField = ({ initialValue, onClear }: { initialValue: string; onClear?: () => void }) => {
    const [value, setValue] = useState(initialValue);
    return <SearchField value={value} onChange={setValue} onClear={onClear} />;
};

describe('SearchField', () => {
    test('reports every keystroke to the parent without debouncing', () => {
        const onChange = vi.fn();
        render(<SearchField value="" onChange={onChange} projectsCount="100" />);

        fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Ktor' } });

        expect(onChange).toHaveBeenCalledWith('Ktor');
    });

    test('commits the current query on Enter', () => {
        const onEnter = vi.fn();
        render(<SearchField value="Ktor" onChange={vi.fn()} onEnter={onEnter} />);

        fireEvent.keyDown(screen.getByRole('textbox'), { key: 'Enter' });

        expect(onEnter).toHaveBeenCalledWith('Ktor');
    });

    test('clears the query with Escape and keeps the field focused', () => {
        const onClear = vi.fn();
        render(<ControlledSearchField initialValue="Koin" onClear={onClear} />);
        const input = screen.getByRole('textbox');

        fireEvent.focus(input);
        fireEvent.keyDown(input, { key: 'Escape' });

        expect(input).toHaveValue('');
        expect(input).toHaveFocus();
        expect(onClear).toHaveBeenCalledOnce();
    });

    test('does not report a change when Escape is pressed on an empty field', () => {
        const onChange = vi.fn();
        render(<SearchField value="" onChange={onChange} />);

        fireEvent.keyDown(screen.getByRole('textbox'), { key: 'Escape' });

        expect(onChange).not.toHaveBeenCalled();
    });

    test('shows and clears the selected category', () => {
        const onCategoryReset = vi.fn();
        render(
            <SearchField
                value=""
                onChange={vi.fn()}
                selectedCategory="Featured"
                onCategoryReset={onCategoryReset}
            />,
        );

        expect(screen.getByRole('textbox')).toHaveAttribute('placeholder', 'Search in Featured');
        fireEvent.keyDown(screen.getByRole('button', { name: 'Clear Featured filter' }), { key: 'Enter' });

        expect(onCategoryReset).toHaveBeenCalledOnce();
    });

    test('focuses the search field with the slash shortcut', () => {
        render(<SearchField value="" onChange={vi.fn()} />);

        fireEvent.keyDown(window, { code: 'Slash' });

        expect(screen.getByRole('textbox')).toHaveFocus();
    });
});
