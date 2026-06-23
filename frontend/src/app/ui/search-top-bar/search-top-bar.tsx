import { useState } from 'react';
import cn from 'classnames';

import styles from './styles.module.css';
import { DropdownMenu, MenuItem } from '@rescui/dropdown-menu';

import { textCn } from '@rescui/typography'
import { SearchParams, SearchSort } from '@/app/types';

interface SearchTopBarProps {
    filters: SearchParams;
    setFilters: (params: SearchParams) => void;
    updateURLFromState: (state: SearchParams) => void;
}

const DEFAULT_SORT: SearchSort = 'relevance';

const SORT_LABELS: Record<SearchSort, string> = {
    'relevance': 'Relevance',
    'most-stars': 'Github stars',
    'most-healthy': 'OSS Health',
    // 'dependents': 'Dependents',
};

export default function SearchTopBar({ filters, setFilters, updateURLFromState }: SearchTopBarProps) {

    const [isOpen, setIsOpen] = useState(false);

    const toggleIsOpen = () => setIsOpen(s => !s);

    const activeSort = filters.sort ?? DEFAULT_SORT;

    const handleSortChange = (sort: SearchSort) => {
        setIsOpen(false);
        if (sort === activeSort) {
            return;
        }
        const newState = { ...filters, page: 1, sort };
        setFilters(newState);
        updateURLFromState(newState);
    };

    return (
        <div className={styles.wrapper}>
            {/*<div className={textCn('rs-h4')}>Found: N results</div>*/}
            <div className={cn(textCn('rs-text-2', { hardness: 'hard' }), styles.sort)}>
                <div>Sort by&nbsp;</div>

                <DropdownMenu
                    isOpen={isOpen}
                    onRequestClose={() => setIsOpen(false)}
                    trigger={<div onClick={toggleIsOpen} className={styles.trigger}>{SORT_LABELS[activeSort]}</div>}
                >
                    {Object.entries(SORT_LABELS).map(([sort, label]) => (
                        <MenuItem
                            key={sort}
                            disabled={sort === activeSort}
                            onClick={() => handleSortChange(sort as SearchSort)}
                        >
                            {label}
                        </MenuItem>
                    ))}
                </DropdownMenu>
            </div>
        </div>
    );
}
