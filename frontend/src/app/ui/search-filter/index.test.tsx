import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { ComponentProps, ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import { SearchParams } from '@/app/types';
import SearchFilter from './index';

vi.mock('@rescui/checkbox', () => ({
    Checkbox: ({ children, checked, onChange }: ComponentProps<'input'> & { children: ReactNode }) => (
        <label>
            <input type="checkbox" checked={checked} onChange={onChange} />
            {children}
        </label>
    ),
}));

vi.mock('@rescui/dropdown', () => ({
    Dropdown: ({ children, isOpen, trigger }: { children: ReactNode; isOpen: boolean; trigger: ReactNode }) => (
        <>{trigger}{isOpen ? children : null}</>
    ),
}));

vi.mock('@rescui/switcher', () => ({
    Switcher: ({ onChange, options, value }: {
        onChange: (value: string) => void;
        options: { label: string; value: string }[];
        value: string;
    }) => (
        <div>
            {options.map(option => (
                <button
                    aria-pressed={value === option.value}
                    key={option.value}
                    onClick={() => onChange(option.value)}
                >
                    {option.label}
                </button>
            ))}
        </div>
    ),
}));

vi.mock('@rescui/radio-button', () => ({
    RadioButton: ({ children }: { children: ReactNode }) => <>{children}</>,
    RadioButtonList: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock('@rescui/icons', () => ({
    FilterIcon: () => null,
    ProjectsIcon: () => null,
}));

vi.mock('@jetbrains/kotlin-web-site-ui/out/components/sidebar-menu', () => ({
    SidebarMenuHeader: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock('@/app/ui/sidebar-mobile/sidebar-mobile', () => ({
    default: ({ children, isOpen }: { children: ReactNode; isOpen: boolean }) => isOpen ? children : null,
}));

vi.mock('@/app/ui/search-field', async () => {
    const { forwardRef } = await import('react');
    type Props = { onChange?: (value: string) => void; onEnter?: (value: string) => void; value?: string };
    return {
        default: forwardRef<HTMLInputElement, Props>(function SearchField({ onChange, onEnter, value }, ref) {
            return (
                <input
                    aria-label="Search"
                    ref={ref}
                    value={value ?? ''}
                    onChange={event => onChange?.(event.target.value)}
                    onKeyDown={event => event.key === 'Enter' && onEnter?.(event.currentTarget.value)}
                />
            );
        }),
    };
});

vi.mock('@/app/analytics', () => ({
    GAEvent: {
        FILTER_DROPDOWN_CLICK: 'filter-dropdown-click',
        FILTER_PLATFORM_CHANGE: 'filter-platform-change',
        SEARCH_MODE_DROPDOWN_CLICK: 'search-mode-dropdown-click',
        SEARCH_MODE_TRIGGER_CHANGE: 'search-mode-trigger-change',
    },
    trackEvent: vi.fn(),
}));

const filters: SearchParams = {
    mode: 'projects',
    page: 3,
    platforms: [],
    query: 'old',
    tags: ['serialization'],
};

const renderFilter = () => {
    const setFilters = vi.fn();
    const updateURLFromState = vi.fn();
    render(
        <SearchFilter
            filters={filters}
            setFilters={setFilters}
            updateURLFromState={updateURLFromState}
        />,
    );
    return { setFilters, updateURLFromState };
};

let filterBottom = 100;

const scrollFilterOutOfView = (outOfView: boolean) => {
    filterBottom = outOfView ? -100 : 100;
    act(() => {
        fireEvent.scroll(window);
    });
};

const compactSearchInput = () =>
    within(screen.getByTestId('compact-search-filter')).getByRole('textbox', { name: 'Search' });

describe('SearchFilter', () => {
    beforeEach(() => {
        filterBottom = 100;
        vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect')
            .mockImplementation(() => ({ bottom: filterBottom } as DOMRect));
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    test('applies the query after the debounce delay and resets paging and tags', () => {
        const { setFilters, updateURLFromState } = renderFilter();

        fireEvent.change(screen.getByRole('textbox', { name: 'Search' }), { target: { value: 'Ktor' } });
        expect(setFilters).not.toHaveBeenCalled();

        act(() => vi.advanceTimersByTime(200));

        const expected = { ...filters, query: 'Ktor', page: 1, tags: [] };
        expect(setFilters).toHaveBeenCalledWith(expected);
        expect(updateURLFromState).toHaveBeenCalledWith(expected, { scroll: false });
    });

    test('searches as you type in the compact sticky bar without Enter', () => {
        const { setFilters } = renderFilter();
        scrollFilterOutOfView(true);

        fireEvent.change(compactSearchInput(), { target: { value: 'Kt' } });
        act(() => vi.advanceTimersByTime(200));

        expect(setFilters).toHaveBeenCalledWith({ ...filters, query: 'Kt', page: 1, tags: [] });
    });

    test('commits the compact bar query immediately on Enter', () => {
        const { setFilters } = renderFilter();
        scrollFilterOutOfView(true);

        fireEvent.change(compactSearchInput(), { target: { value: 'Ktor' } });
        fireEvent.keyDown(compactSearchInput(), { key: 'Enter' });

        expect(setFilters).toHaveBeenCalledOnce();
        expect(setFilters).toHaveBeenCalledWith({ ...filters, query: 'Ktor', page: 1, tags: [] });
    });

    test('hands focus and text over to the main field when the compact bar hides', () => {
        renderFilter();
        scrollFilterOutOfView(true);
        const compactInput = compactSearchInput();

        compactInput.focus();
        fireEvent.change(compactInput, { target: { value: 'Kt' } });
        scrollFilterOutOfView(false);

        expect(screen.queryByTestId('compact-search-filter')).not.toBeInTheDocument();
        const mainInput = screen.getByRole('textbox', { name: 'Search' });
        expect(mainInput).toHaveValue('Kt');
        expect(mainInput).toHaveFocus();
    });

    test('switches from project search to package search', () => {
        const { setFilters, updateURLFromState } = renderFilter();

        fireEvent.click(screen.getByRole('button', { name: 'Packages' }));

        const expected = { ...filters, mode: 'packages' };
        expect(setFilters).toHaveBeenCalledWith(expected);
        expect(updateURLFromState).toHaveBeenCalledWith(expected);
    });

    test('lists target groups and applies a selected platform', () => {
        const { setFilters, updateURLFromState } = renderFilter();

        fireEvent.click(screen.getByRole('button', { name: 'Platforms' }));
        expect(screen.getAllByRole('checkbox')).toHaveLength(6);
        fireEvent.click(screen.getByRole('checkbox', { name: 'iOS' }));

        const expected = { ...filters, platforms: ['ios'], page: 1, tags: [] };
        expect(setFilters).toHaveBeenCalledWith(expected);
        expect(updateURLFromState).toHaveBeenCalledWith(expected);
    });
});
