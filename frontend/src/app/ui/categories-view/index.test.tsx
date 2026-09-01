import { render, screen } from '@testing-library/react';
import { ComponentProps, ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

import { CategoryWithProjects, ProjectSearchResults } from '@/app/types';
import CategoriesView from './index';

vi.mock('@rescui/button', () => ({
    Button: ({ children, icon, iconPosition, mode, size, ...props }: ComponentProps<'button'> & {
        icon?: ReactNode;
        iconPosition?: string;
        mode?: string;
        size?: string;
    }) => {
        void icon;
        void iconPosition;
        void mode;
        void size;
        return <button {...props}>{children}</button>;
    },
}));

vi.mock('@rescui/icons', () => ({
    ArrowRightIcon: () => null,
}));

vi.mock('@rescui/typography', () => ({
    textCn: () => '',
}));

vi.mock('@/app/ui/container', () => ({
    default: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/app/ui/project-card', () => ({
    default: ({ featuredProject }: { featuredProject: ProjectSearchResults }) => (
        <a href={`/project/${featuredProject.ownerLogin}/${featuredProject.name}`}>{featuredProject.name}</a>
    ),
}));

vi.mock('@/app/ui/placeholder-card', () => ({
    default: () => <a href="/faq#how-do-i-add-a-project">Create and submit your own project</a>,
}));

vi.mock('@/app/ui/grant-winner-banner', () => ({
    default: ({ categorySlug }: { categorySlug: string }) => <div>Grant banner: {categorySlug}</div>,
}));

const project = (id: number): ProjectSearchResults => ({
    id,
    name: `Project ${id}`,
    description: `Description ${id}`,
    scmLink: `https://github.com/example/project-${id}`,
    scmStars: id,
    ownerType: 'organization',
    ownerLogin: 'example',
    licenseName: 'Apache-2.0',
    latestReleaseVersion: '1.0.0',
    latestReleasePublishedAtMillis: 0,
    targetGroups: {},
    tags: [],
    markers: [],
});

const category = (name: string, projectCount: number): CategoryWithProjects => ({
    category: { name, markers: [] },
    projects: Array.from({ length: projectCount }, (_, index) => project(index + 1)),
});

describe('CategoriesView', () => {
    test('limits project cards and offers navigation for larger categories', () => {
        render(<CategoriesView categoryWithProjects={[category('Networking', 7)]} />);

        expect(screen.getAllByRole('link', { name: /^Project / })).toHaveLength(6);
        expect(screen.queryByText('Project 7')).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'See All' }))
            .toHaveAttribute('href', '/?category=networking');
        expect(screen.queryByText('Create and submit your own project')).not.toBeInTheDocument();
    });

    test('fills smaller categories with a project submission link', () => {
        render(<CategoriesView categoryWithProjects={[category('Testing', 2)]} />);

        expect(screen.getAllByRole('link', { name: /^Project / })).toHaveLength(2);
        expect(screen.getByRole('link', { name: 'Create and submit your own project' }))
            .toHaveAttribute('href', '/faq#how-do-i-add-a-project');
        expect(screen.queryByRole('link', { name: 'See All' })).not.toBeInTheDocument();
    });

    test('skips empty categories and renders the grant winner banner', () => {
        render(<CategoriesView categoryWithProjects={[
            category('Empty', 0),
            category('Grant Winners', 3),
        ]} />);

        expect(screen.queryByRole('heading', { name: 'Empty' })).not.toBeInTheDocument();
        expect(screen.getByText('Grant banner: grant-winners')).toBeInTheDocument();
    });
});
