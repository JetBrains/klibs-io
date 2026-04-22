"use client";

import React from "react";
import { SearchParams } from "@/app/types";
import SearchFilter from "@/app/ui/search-filter";
import SearchTabFilter from "@/app/ui/search-tab-filter";
import { useRouter } from "next/navigation";

interface SearchContainerProps {
    filters: SearchParams;
    setFilters: (params: SearchParams) => void;
    hideTagsFilter?: boolean;
    selectedCategory?: string | null;
    onCategoryReset?: () => void;
    categorySearchQuery?: string;
    onCategorySearch?: (query: string) => void;
    onCategorySearchClear?: () => void;
    projectsCount?: string;
}

export default function SearchContainer({ filters, setFilters, hideTagsFilter, selectedCategory, onCategoryReset, categorySearchQuery, onCategorySearch, onCategorySearchClear, projectsCount }: SearchContainerProps) {
    const router = useRouter();

    const updateURLFromState = (state: SearchParams) => {
        const newSearchParams = new URLSearchParams();
        if (state.mode === 'packages') newSearchParams.set('mode', state.mode);
        if (state.query) newSearchParams.set('query', state.query);
        if (state.platforms && state.platforms.length > 0) {
            state.platforms.forEach((platform) => newSearchParams.append('platforms', platform));
        }
        if (state.sort) newSearchParams.set('sort', state.sort);
        if (state.page && state.page > 1) newSearchParams.set('page', state.page.toString());
        if (state.limit) newSearchParams.set('limit', state.limit.toString());
        if (state.tags && state.tags.length > 0) {
            state.tags.forEach((tag) => newSearchParams.append('tags', tag));
        }
        router.push(`/?${newSearchParams.toString()}`);
    };

    return (
        <>
            <SearchFilter
                filters={filters}
                setFilters={setFilters}
                updateURLFromState={updateURLFromState}
                selectedCategory={selectedCategory}
                onCategoryReset={onCategoryReset}
                categorySearchQuery={categorySearchQuery}
                onCategorySearch={onCategorySearch}
                onCategorySearchClear={onCategorySearchClear}
                projectsCount={projectsCount}
            />
            {!hideTagsFilter && (
                <SearchTabFilter
                    filters={filters}
                    setFilters={setFilters}
                    updateURLFromState={updateURLFromState}
                />
            )}
        </>
    );
}
