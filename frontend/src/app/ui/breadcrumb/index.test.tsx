import { render, screen } from '@testing-library/react';
import { ComponentProps, forwardRef, ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { packageDetails, packageOverview, packageVersionHistory, projectDetails } from '@/test/fixtures';
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
    const [latest, previous] = packageVersionHistory();

    test('shows the latest released version when the url has none', () => {
        renderBreadcrumbs([latest, previous]);

        expect(screen.getByRole('button')).toHaveTextContent(latest.version);
    });

    test('shows the requested version and keeps the release order in the menu', () => {
        renderBreadcrumbs([latest, previous], previous.version);

        expect(screen.getByRole('button')).toHaveTextContent(previous.version);
        expect(screen.getAllByRole('link', { name: /^1\./ }).map(link => link.textContent))
            .toEqual([latest.version, previous.version]);
    });
});
