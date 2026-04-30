"use client";

import React from "react";
import cn from "classnames";

import { Select } from '@rescui/select';
import { textCn } from '@rescui/typography';

import {
    DEFAULT_PROJECT_SORT,
    PROJECT_SORT_OPTIONS,
    SearchParams,
    SearchSort,
} from "@/app/types";
import { trackEvent, GAEvent } from "@/app/analytics";

import styles from "./styles.module.css";

interface SearchSortProps {
    filters: SearchParams;
    setFilters: (params: SearchParams) => void;
    updateURLFromState: (state: SearchParams) => void;
}

export default function SearchSortSelect({ filters, setFilters, updateURLFromState }: SearchSortProps) {
    const currentSort = filters.sort || DEFAULT_PROJECT_SORT;
    const currentOption =
        PROJECT_SORT_OPTIONS.find((option) => option.value === currentSort) ||
        PROJECT_SORT_OPTIONS[0];

    const handleChange = (option: { label: string; value?: string }) => {
        const nextSort = option.value as SearchSort;
        if (nextSort === currentSort) return;

        const newState: SearchParams = { ...filters, sort: nextSort, page: 1 };
        setFilters(newState);
        updateURLFromState(newState);

        trackEvent(GAEvent.FILTER_SORT_CHANGE, {
            eventCategory: nextSort,
        });
    };

    return (
        <div className={styles.wrapper} data-testid="search-sort">
            <span className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.label)}>
                Sort by:
            </span>
            <Select
                className={styles.select}
                mode="rock"
                size="m"
                options={PROJECT_SORT_OPTIONS}
                value={currentOption}
                onChange={handleChange}
            />
        </div>
    );
}
