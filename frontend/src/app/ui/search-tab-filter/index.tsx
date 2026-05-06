"use client";

import React, {useCallback, useEffect, useMemo, useState} from "react";
import cn from "classnames";

import styles from "./styles.module.css";

import {DownIcon, UpIcon} from '@rescui/icons'
import {ChipList, Chip} from '@rescui/chip-list';
import {textCn} from "@rescui/typography";

import Container from "@/app/ui/container";
import {useWindowWidth} from "@/app/hooks";
import {BREAKPOINTS} from "@/app/media";
import {SearchParams, TagsStats} from "@/app/types";
import {getTagsStats} from "@/app/api";
import {Button} from "@rescui/button";

const TAGS_TABS_LIMIT = 50;
const VISIBLE_SKELETON_TAGS = 8;
const VISIBLE_TAGS_DESKTOP = 8;
const VISIBLE_TAGS_TABLET = 5;

interface SearchTabFilterProps {
    filters: SearchParams;
    setFilters: (params: SearchParams) => void;
    updateURLFromState: (state: SearchParams) => void;
}

export default function SearchTabFilter({filters, setFilters, updateURLFromState}: SearchTabFilterProps) {
    const windowWidth = useWindowWidth();

    const [tagsStats, setTagsStats] = useState<TagsStats | null>(null);
    const [isFetchingTagsStats, setIsFetchingTagsStats] = useState<boolean>(true);
    const [selectedTagTabIndex, setSelectedTagTabIndex] = useState<number>(0);

    const [isExpanded, setIsExpanded] = useState<boolean>(false);

    const defaultVisibleTags = windowWidth && windowWidth >= BREAKPOINTS.xl ? VISIBLE_TAGS_DESKTOP : VISIBLE_TAGS_TABLET;

    const fetchTagsStats = useCallback(async () => {
        try {
            const tagsStatsData = await getTagsStats({limit: TAGS_TABS_LIMIT});
            setTagsStats(tagsStatsData);
        } catch (e) {
            // silent
        }
    }, []);

    useEffect(() => {
        setIsFetchingTagsStats(true);
        fetchTagsStats().finally(() => setIsFetchingTagsStats(false));
    }, [fetchTagsStats]);

    useEffect(() => {
        if (filters.tags && filters.tags.length > 0 && tagsStats) {
            const selectedIdx = tagsStats.tags.findIndex(({tag}) => tag === filters.tags![0]);
            setSelectedTagTabIndex(selectedIdx === -1 ? 0 : selectedIdx + 1);
        } else {
            setSelectedTagTabIndex(0);
        }
    }, [filters.tags, tagsStats]);

    const handleTagTabChange = (tagTabIndex: number) => {
        setSelectedTagTabIndex(tagTabIndex);
        const selectedTag = tagTabIndex === 0 ? null : tagsStats?.tags[tagTabIndex - 1].tag;
        const newTags = selectedTag ? [selectedTag] : [];
        const newState = {...filters, page: 1, tags: newTags};
        setFilters(newState);
        updateURLFromState(newState);
    };

    const shouldHide = useMemo(() => {
        return !!(filters.query) || !!(filters.platforms && filters.platforms.length > 0) || filters.mode === 'packages';
    }, [filters.query, filters.platforms, filters.mode]);

    const isLoading = isFetchingTagsStats || !tagsStats;


    const handleShowMoreButtonClick = () => {
        setIsExpanded(!isExpanded);
    }

    const visibleTagsCount = isExpanded ? TAGS_TABS_LIMIT : defaultVisibleTags;

    // Check if selected tag is in the hidden area (collapsed view only)
    const isSelectedTagHidden = !isExpanded && selectedTagTabIndex > visibleTagsCount;
    const hiddenSelectedTag = isSelectedTagHidden ? tagsStats?.tags[selectedTagTabIndex - 1] : null;

    if (shouldHide) return null;

    return (
        <Container className={cn("mt-3", styles.mainWrapper)}>

            <h2 className={cn(textCn('rs-h2'), styles.title)}>Top tags</h2>

            {isLoading ? (
                <div className={styles.skeletonContainer}>
                    {Array.from({length: VISIBLE_SKELETON_TAGS}, (_, index) => (
                        <span key={index} className={styles.skeletonTag} />
                    ))}
                </div>
            ) : (
                <ChipList
                    data-testid="top-tags-chip-list"
                    value={hiddenSelectedTag ? visibleTagsCount : selectedTagTabIndex}
                    onChange={(index: number) => {
                        const isHiddenTagClicked = hiddenSelectedTag && index === visibleTagsCount;
                        handleTagTabChange(isHiddenTagClicked ? selectedTagTabIndex : index);
                    }}
                    mode={'rock'}
                    compact
                >
                    <Chip>All</Chip>
                    {tagsStats?.tags.slice(0, hiddenSelectedTag ? visibleTagsCount - 1 : visibleTagsCount).map(({tag}) => (
                        <Chip key={tag}>
                            {tag}
                        </Chip>
                    ))}
                    {hiddenSelectedTag && (
                        <Chip key={hiddenSelectedTag.tag}>
                            {hiddenSelectedTag.tag}
                        </Chip>
                    )}
                    <Button
                        data-testid="top-tags-show-more-button"
                        mode={'clear'}
                        size={'m'}
                        iconPosition={'right'}
                        icon={isExpanded ? <UpIcon/> : <DownIcon/>}
                        onClick={handleShowMoreButtonClick}
                    >
                        {isExpanded ? 'Show less' : 'Show more'}
                    </Button>

                </ChipList>
            )}

        </Container>
    );
}
