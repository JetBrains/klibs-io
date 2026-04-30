"use client";

import React from "react";
import { SearchParams } from "@/app/types";
import SearchFilter from "@/app/ui/search-filter";
import SearchTabFilter from "@/app/ui/search-tab-filter";

interface SearchContainerProps {
    filters: SearchParams;
    setFilters: (params: SearchParams) => void;
    updateURLFromState: (state: SearchParams) => void;
    hideTagsFilter?: boolean;
    selectedCategory?: string | null;
    onCategoryReset?: () => void;
    categorySearchQuery?: string;
    onCategorySearch?: (query: string) => void;
    onCategorySearchClear?: () => void;
    projectsCount?: string;
}

export default function SearchContainer({ filters, setFilters, updateURLFromState, hideTagsFilter, selectedCategory, onCategoryReset, categorySearchQuery, onCategorySearch, onCategorySearchClear, projectsCount }: SearchContainerProps) {
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
