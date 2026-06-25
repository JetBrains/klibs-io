import { useState } from 'react';
import cn from 'classnames';
import Link from 'next/link';

import styles from './styles.module.css';
import { DropdownMenu, MenuItem } from '@rescui/dropdown-menu';
import { Tooltip } from '@rescui/tooltip';

import { textCn } from '@rescui/typography'
import { SearchParams, SearchSort } from '@/app/types';
import { trackEvent, GAEvent } from '@/app/analytics';

interface SearchTopBarProps {
    filters: SearchParams;
    setFilters: (params: SearchParams) => void;
    updateURLFromState: (state: SearchParams) => void;
}

const DEFAULT_SORT: SearchSort = 'relevance';

const OSS_HEALTH_HINT = 'A 0–100 score of how actively the project is maintained on GitHub.';
const OSS_HEALTH_LEARN_MORE_HREF = '/faq#oss-health';

const OSS_HEALTH_TOOLTIP = (
    <span className={styles.ossHealthTooltip}>
        {OSS_HEALTH_HINT}{' '}<br />
        <Link href={OSS_HEALTH_LEARN_MORE_HREF} className="link-secondary">How it&apos;s calculated</Link>
    </span>
);

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
        trackEvent(GAEvent.SEARCH_SORT_CHANGE, {
            eventCategory: sort,
        });
        const newState = { ...filters, page: 1, sort };
        setFilters(newState);
        updateURLFromState(newState);
    };

    return (
        <div className={styles.wrapper}>
            <div className={cn(textCn('rs-text-2', { hardness: 'hard' }), styles.sort)}>
                <div>Sort by&nbsp;</div>

                <DropdownMenu
                    isOpen={isOpen}
                    placement={'bottom-end'}
                    onRequestClose={() => setIsOpen(false)}
                    trigger={
                        <div
                            role="button"
                            tabIndex={0}
                            aria-haspopup="menu"
                            aria-expanded={isOpen}
                            onClick={toggleIsOpen}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    toggleIsOpen();
                                }
                            }}
                            className={styles.trigger}
                        >
                            {SORT_LABELS[activeSort]}
                        </div>
                    }
                >
                    {Object.entries(SORT_LABELS).map(([sort, label]) => {
                        const menuItem = (
                            <MenuItem
                                key={sort}
                                className={cn({[styles.active] : sort === activeSort})}
                                onClick={() => handleSortChange(sort as SearchSort)}
                            >
                                {label}
                            </MenuItem>
                        );

                        if (sort === 'most-healthy') {
                            return (
                                <Tooltip sparse={false} key={sort} placement="left" content={OSS_HEALTH_TOOLTIP}>
                                    {menuItem}
                                </Tooltip>
                            );
                        }

                        return menuItem;
                    })}
                </DropdownMenu>
            </div>
        </div>
    );
}
