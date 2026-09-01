import { fireEvent, render, screen } from '@testing-library/react';
import { ComponentProps, ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

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

vi.mock('@rescui/typography', () => ({
    textCn: () => '',
}));

vi.mock('@jetbrains/kotlin-web-site-ui/out/components/sidebar-menu', () => ({
    SidebarMenuHeader: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock('@/app/ui/container', () => ({
    default: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/app/ui/sidebar-mobile/sidebar-mobile', () => ({
    default: ({ children, isOpen }: { children: ReactNode; isOpen: boolean }) => isOpen ? children : null,
}));

vi.mock('@/app/ui/search-field', () => ({
    default: ({ onChange, value }: { onChange?: (value: string) => void; value?: string }) => (
        <input
            aria-label="Search"
            value={value ?? ''}
            onChange={event => onChange?.(event.target.value)}
        />
    ),
}));

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

describe('SearchFilter', () => {
    test('resets paging and tags when the query changes', () => {
        const { setFilters, updateURLFromState } = renderFilter();

        fireEvent.change(screen.getByRole('textbox', { name: 'Search' }), { target: { value: 'Ktor' } });

        const expected = { ...filters, query: 'Ktor', page: 1, tags: [] };
        expect(setFilters).toHaveBeenCalledWith(expected);
        expect(updateURLFromState).toHaveBeenCalledWith(expected);
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
