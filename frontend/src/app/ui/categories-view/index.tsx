"use client"

import React from "react";
import Link from "next/link";
import { Category, ProjectSearchResults, CategoryWithProjects } from "@/app/types";
import ProjectCard from "@/app/ui/project-card";
import PlaceholderCard from "@/app/ui/placeholder-card";
import Container from "@/app/ui/container";
import { Button } from "@rescui/button";
import { textCn } from '@rescui/typography';
import { ArrowRightIcon } from '@rescui/icons';
import GrantWinnerBanner from "@/app/ui/grant-winner-banner";

import { toCategorySlug } from "@/app/helpers";
import { GRANT_WINNERS_SLUG } from "@/app/constants";

import styles from './styles.module.css';

const CATEGORY_ITEMS_LIMIT = 6;

interface CategorySectionProps {
    category: Category;
    projects: ProjectSearchResults[];
}

function CategorySection({ category, projects }: CategorySectionProps) {
    const categorySlug = toCategorySlug(category.name);
    const isGrantWinners = categorySlug === GRANT_WINNERS_SLUG;

    if (isGrantWinners) {
        return (
            <div className={styles.categoryWrapper}>
                <GrantWinnerBanner categorySlug={categorySlug} />
            </div>
        );
    }

    if (projects.length === 0) {
        return null;
    }

    return (
        <div className={styles.categoryWrapper} data-testid={`category-section-${categorySlug}`}>
            <div className={styles.categoryHeader} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2 className={textCn('rs-h2')}>{category.name}</h2>
                {projects.length > CATEGORY_ITEMS_LIMIT && (
                    <Link href={`/?category=${categorySlug}`}>
                        <Button
                            data-testid={`see-all-button-${categorySlug}`}
                            size="m"
                            mode="clear"
                            icon={<ArrowRightIcon />}
                            iconPosition={'right'}
                            tabIndex={-1}
                        >
                            See All
                        </Button>
                    </Link>
                )}
            </div>
            <Container mode="wrapper" cardGrid>
                {projects.slice(0, CATEGORY_ITEMS_LIMIT).map((project) => (
                    <ProjectCard key={project.id} featuredProject={project} />
                ))}
                {projects.length < CATEGORY_ITEMS_LIMIT && <PlaceholderCard />}
            </Container>
        </div>
    );
}

interface CategoriesViewProps {
    categoryWithProjects: CategoryWithProjects[];
}

export default function CategoriesView({ categoryWithProjects }: CategoriesViewProps) {
    if (categoryWithProjects.length === 0) {
        return null;
    }

    return (
        <div>
            {categoryWithProjects.map(({ category, projects }) => (
                <CategorySection
                    key={category.name}
                    category={category}
                    projects={projects}
                />
            ))}
        </div>
    );
}
