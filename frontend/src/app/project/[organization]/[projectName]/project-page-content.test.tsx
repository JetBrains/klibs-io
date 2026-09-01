import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { projectDetails } from '@/test/fixtures';
import Project from './project-page-content';

vi.mock('@rescui/card', () => ({ cardCn: () => '' }));
vi.mock('@rescui/typography', () => ({ textCn: () => '' }));
vi.mock('@rescui/tab-list', () => ({
    Tab: ({ children }: { children: ReactNode }) => <button>{children}</button>,
    TabList: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    TabSeparator: () => null,
}));

vi.mock('@/app/ui/breadcrumb', () => ({ ProjectBreadcrumb: () => null }));
vi.mock('@/app/ui/container', () => ({
    default: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));
vi.mock('@/app/ui/package-importer-dropdown', () => ({ PackageImporterDropdown: () => null }));
vi.mock('@/app/ui/project-info', () => ({ ProjectInfo: () => null }));
vi.mock('@/app/ui/side-popup', () => ({
    default: ({ target }: { target: ReactNode }) => <>{target}</>,
}));
vi.mock('@/app/ui/tags', () => ({
    default: ({ tags }: { tags: string[] }) => <div>{tags.join(', ')}</div>,
}));
vi.mock('@/app/ui/targets-list', () => ({ default: () => null }));
vi.mock('@/app/ui/targets-table-popup', () => ({ default: () => null }));
vi.mock('@/app/analytics', () => ({
    GAEvent: {
        PROJECT_INFO_LINK_CLICK: 'project-info-link-click',
        PROJECT_PACKAGES_TAB_CLICK: 'project-packages-tab-click',
        PROJECT_PACKAGE_CLICK: 'project-package-click',
        PROJECT_README_TAB_CLICK: 'project-readme-tab-click',
    },
    trackEvent: vi.fn(),
}));

describe('Project page content', () => {
    beforeEach(() => {
        Object.defineProperty(window, 'scroll', { configurable: true, value: vi.fn() });
    });

    test('builds the project metadata edit URL', () => {
        render(
            <Project
                initialProject={projectDetails()}
                initialPackages={[]}
                initialReadme=""
                projectName="Arrow"
            />,
        );

        const suggestEditUrl = new URL(screen.getByRole('link', { name: 'Suggest an edit' }).getAttribute('href')!);
        expect(suggestEditUrl.searchParams.get('url')).toBe('https://klibs.io/project/arrow-kt/Arrow');
        expect(suggestEditUrl.searchParams.get('template')).toBe('suggest_an_edit.yml');
    });

    test('warns when the project repository is archived', () => {
        render(
            <Project
                initialProject={projectDetails({ archived: true, archivedAtMillis: Date.UTC(2025, 0, 2) })}
                initialPackages={[]}
                initialReadme=""
                projectName="Arrow"
            />,
        );

        expect(screen.getByText(
            "The project's repository was archived on Jan 2, 2025. The project will not receive updates.",
        )).toBeInTheDocument();
    });
});
