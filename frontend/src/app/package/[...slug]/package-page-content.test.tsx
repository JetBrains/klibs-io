import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { packageDetails, packageOverview, projectDetails } from '@/test/fixtures';
import Package from './package-page-content';

vi.mock('@rescui/icons', () => ({
    CompanyIcon: () => null, FilesIcon: () => null, GearIcon: () => null,
    GlobusIcon: () => null, KotlinIcon: () => null, RocketIcon: () => null, TeamIcon: () => null,
}));
vi.mock('@rescui/table', () => ({ tableCn: () => '' }));
vi.mock('@rescui/typography', () => ({ textCn: () => '' }));
vi.mock('@rescui/tab-list', () => ({
    Tab: ({ children }: { children: ReactNode }) => <button>{children}</button>,
    TabList: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));
vi.mock('@/app/ui/breadcrumb', () => ({ PackageBreadcrumbs: () => null }));
vi.mock('@/app/ui/container', () => ({
    default: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));
vi.mock('@/app/ui/package-importer', () => ({ PackageImportCode: () => null }));
vi.mock('@/app/ui/package-importer-dropdown', () => ({ PackageImporterDropdown: () => null }));
vi.mock('@/app/ui/side-popup', () => ({ default: ({ target }: { target: ReactNode }) => <>{target}</> }));
vi.mock('@/app/ui/targets-list', () => ({ default: () => null }));
vi.mock('@/app/ui/targets-table', () => ({ default: () => null }));
vi.mock('@/app/ui/targets-table-popup', () => ({ default: () => null }));
vi.mock('@/app/ui/time-ago', () => ({
    default: ({ timestamp }: { timestamp: number }) => <>{timestamp}</>,
    TimestampToDate: ({ timestamp }: { timestamp: number }) => <>{timestamp}</>,
}));
vi.mock('@/app/analytics', () => ({
    GAEvent: {
        PACKAGE_PAGE_LINK_CLICK: 'package-page-link-click',
        PACKAGE_VERSION_LINK_CLICK: 'package-version-link-click',
    },
    trackEvent: vi.fn(),
}));

const renderPackage = ({
    currentPackage = packageDetails(),
    parentProject = projectDetails(),
    versions = [],
    version,
}: {
    currentPackage?: ReturnType<typeof packageDetails>;
    parentProject?: ReturnType<typeof projectDetails> | null;
    versions?: ReturnType<typeof packageOverview>[];
    version?: string;
} = {}) => render(
    <Package
        initialPackage={currentPackage}
        initialParentProject={parentProject}
        initialPackageVersions={versions}
        initialGroupArtifacts={[]}
        version={version}
    />,
);

describe('Package page content', () => {
    beforeEach(() => {
        Object.defineProperty(window, 'scroll', { configurable: true, value: vi.fn() });
    });

    test('links to the parent project when one exists', () => {
        renderPackage();

        expect(screen.getByRole('link', { name: /Arrow \(6.0k stars\)/ }))
            .toHaveAttribute('href', '/project/arrow-kt/Arrow');
    });

    test('renders unavailable metadata as text without empty links', () => {
        renderPackage({
            currentPackage: packageDetails({
                developers: [{ title: 'Arrow maintainers', url: '' }],
                licenses: [{ title: 'Apache-2.0', url: '' }],
                linkFiles: null,
                linkHomepage: null,
                linkScm: '',
            }),
            parentProject: null,
        });

        expect(screen.getByText('Apache-2.0')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Apache-2.0' })).not.toBeInTheDocument();
        expect(screen.getByText('Arrow maintainers')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Arrow maintainers' })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /Homepage|Source code management|Maven artifacts/ }))
            .not.toBeInTheDocument();
    });

    test('links every version and marks the current one', () => {
        renderPackage({
            versions: [
                packageOverview(),
                packageOverview({ id: 2, version: '1.0.0' }),
            ],
            version: '2.0.0',
        });

        for (const link of screen.getAllByRole('link', { name: '2.0.0' })) {
            expect(link).toHaveAttribute('href', '/package/io.arrow-kt/arrow-core/2.0.0');
            expect(link).toHaveAttribute('aria-current', 'page');
        }
        for (const link of screen.getAllByRole('link', { name: '1.0.0' })) {
            expect(link).toHaveAttribute('href', '/package/io.arrow-kt/arrow-core/1.0.0');
            expect(link).not.toHaveAttribute('aria-current');
        }
    });
});
