import { render, screen } from '@testing-library/react';
import { ComponentProps, forwardRef, ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { packageDetails, packageOverview, projectDetails } from '@/test/fixtures';
import { PackageBreadcrumbs } from './index';

vi.mock('next/navigation', () => ({
    usePathname: () => '/package/io.arrow-kt/arrow-core',
    useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@rescui/button', () => ({
    Button: forwardRef<HTMLButtonElement, ComponentProps<'button'> & { icon?: ReactNode }>(
        function Button({ icon, children, ...props }, ref) {
            void icon;
            return <button ref={ref} {...props}>{children}</button>;
        },
    ),
}));

vi.mock('@rescui/icons', () => ({ DownIcon: () => null, LoadingIcon: () => null }));

vi.mock('@rescui/dropdown-menu', () => ({
    DropdownMenu: ({ trigger, children }: { trigger: ReactNode; children: ReactNode }) => <>{trigger}{children}</>,
}));

vi.mock('@rescui/menu', () => ({
    MenuItem: ({ href, children }: { href: string; children: ReactNode }) => <a href={href}>{children}</a>,
}));

const renderBreadcrumbs = (versions: ReturnType<typeof packageOverview>[], version?: string) => render(
    <PackageBreadcrumbs
        projectPackage={packageDetails()}
        projectPackages={[packageOverview()]}
        packageVersions={versions}
        parentProject={projectDetails()}
        version={version}
    />,
);

describe('PackageBreadcrumbs', () => {
    test('shows the latest released version when the url has none', () => {
        renderBreadcrumbs([
            packageOverview({ id: 1, version: '1.10.2', releasedAtMillis: 1_700_000_000_000 }),
            packageOverview({ id: 2, version: '1.9.0-RC.2', releasedAtMillis: 1_600_000_000_000 }),
        ]);

        expect(screen.getByRole('button')).toHaveTextContent('1.10.2');
    });

    test('shows the requested version and keeps the release order in the menu', () => {
        renderBreadcrumbs([
            packageOverview({ id: 1, version: '1.10.2', releasedAtMillis: 1_700_000_000_000 }),
            packageOverview({ id: 2, version: '1.9.0-RC.2', releasedAtMillis: 1_600_000_000_000 }),
        ], '1.9.0-RC.2');

        expect(screen.getByRole('button')).toHaveTextContent('1.9.0-RC.2');
        expect(screen.getAllByRole('link', { name: /^1\./ }).map(link => link.textContent))
            .toEqual(['1.10.2', '1.9.0-RC.2']);
    });
});
