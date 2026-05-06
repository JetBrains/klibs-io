"use client"

import React, {Suspense, useState, useEffect} from "react";
import {
    Category,
    CategoryWithProjects,
    Platform, SearchMode,
    SearchParams,
    SearchSort
} from "@/app/types";
import {useSearchParams, useRouter} from "next/navigation";
import { toCategorySlug } from "@/app/helpers";

import SearchContainer from "@/app/ui/search-container";
import {SearchResults} from "@/app/ui/search-results/search-results";
import CategoriesView from "@/app/ui/categories-view";
import CategoryResults from "@/app/ui/category-results";
import Container from "@/app/ui/container";

import FocusManager from '@rescui/focus-manager';

interface PageContentProps {
    categories: Category[];
    categoryWithProjects: CategoryWithProjects[];
    projectsCount: string;
}

function hasActiveFilters(filters: SearchParams): boolean {
    return !!(
        filters.query ||
        (filters.platforms && filters.platforms.length > 0) ||
        (filters.tags && filters.tags.length > 0) ||
        filters.mode === 'packages'
    );
}

function FilterWithResults({ categories, categoryWithProjects, projectsCount }: PageContentProps) {
    const searchParams = useSearchParams();
    const router = useRouter();
    const [filters, setFilters] = useState<SearchParams>(
        () => getSearchParamsFromUrl(searchParams)
    );

    const categorySlug = searchParams.get("category");
    const selectedCategory = categorySlug
        ? categories.find(c => toCategorySlug(c.name) === categorySlug)
        : null;

    // Category search state (separate from main search) - initialize from URL
    const [categorySearchQuery, setCategorySearchQuery] = useState<string>(
        () => selectedCategory ? (searchParams.get("query") || "") : ""
    );

    // Redirect to home if category slug is invalid. This is up to discussion how to handle this scenario
    useEffect(() => {
        if (categorySlug && !selectedCategory) {
            router.replace('/');
        }
    }, [categorySlug, selectedCategory, router]);

    // Reset category search query when leaving category view
    useEffect(() => {
        if (!selectedCategory) {
            setCategorySearchQuery("");
        }
    }, [selectedCategory]);

    const showCategoriesView = !hasActiveFilters(filters) && !categorySlug;

    const updateCategoryURL = (query: string) => {
        const newSearchParams = new URLSearchParams();
        newSearchParams.set('category', categorySlug!);
        if (query) {
            newSearchParams.set('query', query);
        }
        router.push(`/?${newSearchParams.toString()}`);
    };

    const handleCategoryReset = () => {
        setCategorySearchQuery("");
        router.push('/');
    };

    const handleCategorySearch = (query: string) => {
        setCategorySearchQuery(query);
        updateCategoryURL(query);
    };

    const handleCategorySearchClear = () => {
        setCategorySearchQuery("");
        updateCategoryURL("");
    };

    return (
        <>
            <SearchContainer
                filters={filters}
                setFilters={setFilters}
                hideTagsFilter={!!selectedCategory}
                selectedCategory={selectedCategory?.name}
                onCategoryReset={handleCategoryReset}
                categorySearchQuery={categorySearchQuery}
                onCategorySearch={handleCategorySearch}
                onCategorySearchClear={handleCategorySearchClear}
                projectsCount={projectsCount}
            />

            <Container mode={"container"} className="padding-top-small padding-bottom-large">
                {selectedCategory ? (
                    <CategoryResults category={selectedCategory} searchQuery={categorySearchQuery} />
                ) : showCategoriesView ? (
                    <CategoriesView categoryWithProjects={categoryWithProjects} />
                ) : (
                    <SearchResults filters={filters} isPackageSearch={filters.mode === 'packages'}/>
                )}
            </Container>
        </>
    );
}

export default function PageContent({ categories, categoryWithProjects, projectsCount }: PageContentProps) {

    useEffect(() => {
        const focusManager = new FocusManager();
        return () => {
            focusManager.destroy();
        };
    }, []);

    return (
        <Suspense>
            <FilterWithResults categories={categories} categoryWithProjects={categoryWithProjects} projectsCount={projectsCount} />
        </Suspense>
    );
}

function getSearchParamsFromUrl(
    searchParams: URLSearchParams
): SearchParams {
    const params: SearchParams = {
        page: 1,
    };

    const query = searchParams.get("query") || "";
    const platforms = searchParams.getAll("platforms") || [];
    const sort = searchParams.get("sort") || "";
    const tags = searchParams.getAll("tags") || [];
    const mode = searchParams.get("mode") || "";

    if (query) {
        params.query = query;
    }
    if (platforms.length > 0) {
        params.platforms = platforms as Platform[];
    }
    if (sort) {
        params.sort = sort as SearchSort;
    }
    if (tags.length > 0) {
        params.tags = tags;
    }

    if (mode) {
        params.mode = mode as SearchMode;
    }

    return params;
}
