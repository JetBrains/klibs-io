import {
    getPlatformName,
    Platform,
    platformOrder,
    sortedPlatforms,
    SearchMode,
    SearchParams
} from "@/app/types";
import React, {useState, useRef, useEffect} from "react";
import styles from './styles.module.css';

import {Checkbox} from '@rescui/checkbox';
import {Dropdown} from '@rescui/dropdown';
import {Switcher} from '@rescui/switcher';
import {RadioButton, RadioButtonList} from '@rescui/radio-button';

import cn from "classnames";
import Container from "@/app/ui/container";
import SearchField from "@/app/ui/search-field";
import {DropdownTrigger} from "@/app/ui/dropdown-trigger";

import {FilterIcon, ProjectsIcon} from "@rescui/icons";

import {SidebarMenuHeader} from '@jetbrains/kotlin-web-site-ui/out/components/sidebar-menu';
import {textCn} from '@rescui/typography';

import SidebarMobile from "@/app/ui/sidebar-mobile/sidebar-mobile";

interface SearchFilterProps {
    filters: SearchParams,
    setFilters: (params: SearchParams) => void;
    updateURLFromState: (state: SearchParams) => void;
    selectedCategory?: string | null;
    onCategoryReset?: () => void;
    categorySearchQuery?: string;
    onCategorySearch?: (query: string) => void;
    onCategorySearchClear?: () => void;
    projectsCount?: string;
}

import {trackEvent, GAEvent} from "@/app/analytics";


export default function SearchFilter({filters, setFilters, updateURLFromState, selectedCategory, onCategoryReset, categorySearchQuery, onCategorySearch, onCategorySearchClear, projectsCount}: SearchFilterProps) {

    const [textQuery, setTextQuery] = useState<string>(filters.query || "");
    const [selectedPlatforms, setSelectedPlatforms] = useState<Platform[]>(
        filters.platforms || []
    );
    const searchFilterContainerRef = useRef<HTMLDivElement>(null);
    const [showCompactFilter, setShowCompactFilter] = useState(false);

    useEffect(() => {
        const onScroll = () => {
            if (searchFilterContainerRef.current) {
                const rect = searchFilterContainerRef.current.getBoundingClientRect();
                const elementBottom = rect.bottom + window.scrollY;
                setShowCompactFilter(window.scrollY > elementBottom);
            }
        };
        onScroll();
        window.addEventListener('scroll', onScroll);
        return () => {
            window.removeEventListener('scroll', onScroll);
        };
    }, []);

    const handleTextQueryChange = (value: string) => {
        const newQuery = value;
        setTextQuery(newQuery);

        const newState = {...filters, query: newQuery, page: 1, tags: []};
        setFilters(newState);

        updateURLFromState(newState);
    };

    const handlePlatformFilterChange = (platform: Platform) => {

        trackEvent(GAEvent.FILTER_PLATFORM_CHANGE, {
            eventCategory: platform,
        });

        const newSelectedPlatforms = selectedPlatforms.includes(platform)
            ? selectedPlatforms.filter((p) => p !== platform)
            : [...selectedPlatforms, platform];

        setSelectedPlatforms(newSelectedPlatforms);

        const newState = {...filters, platforms: newSelectedPlatforms, page: 1, tags: []};
        setFilters(newState);

        updateURLFromState(newState);
    };

    const [showPlatformDropdown, setShowPlatformDropdown] = useState<boolean>(false);
    const handleClosePlatformDropdown = () => setShowPlatformDropdown(false);
    const handlePlatformDropdownClick = () => setShowPlatformDropdown(showPlatformDropdown => !showPlatformDropdown);

    const [searchMode, setSearchMode] = React.useState<SearchMode>(filters.mode || "projects");

    const handleSearchModeChange = (value: string) => {
        setSearchMode(value as SearchMode);
        const newState = {...filters, mode: value as SearchMode};
        setFilters(newState);
        updateURLFromState(newState);
        trackEvent(GAEvent.SEARCH_MODE_TRIGGER_CHANGE, {
            eventCategory: value
        });
    }

    const [mobileMenuVisible, setMobileMenuVisible] = useState<boolean>(false);

    const handleSidebarClose = () => {
        setMobileMenuVisible(false);
    }

    const handleSidebarOpen = () => {
        setMobileMenuVisible(true);
    }


    const filteredPlatforms = sortedPlatforms(platformOrder);

    return (
        <>
            {/*Mobile filters sidebar*/}
            <SidebarMobile
                isOpen={mobileMenuVisible}
                onClose={handleSidebarClose}
            >
                <SidebarMenuHeader>
                    <div className={cn(textCn('rs-text-2'), styles.sidebarHeader)}>Filter</div>
                </SidebarMenuHeader>
                <div className={styles.sidebarFilterMobileGroup}>
                    <h5 className={cn(styles.mobileSectionHeading, textCn('rs-text-3'))}>Search mode</h5>
                    <div>
                        <RadioButtonList
                            theme={'dark'}
                            mode={'rock'}
                            size={'l'}
                            defaultValue={searchMode}
                            onChange={handleSearchModeChange}
                        >
                            <RadioButton value="projects">KMP projects</RadioButton>
                            <RadioButton value="packages">KMP packages</RadioButton>
                        </RadioButtonList>
                    </div>
                </div>

                <div className={styles.sidebarFilterMobileGroup}>
                    <h5 className={cn(styles.mobileSectionHeading, textCn('rs-text-3'))}>Target group filter</h5>
                    <div>
                        {filteredPlatforms.map((platformId) => (
                            <Checkbox
                                theme="dark"
                                mode="rock"
                                size={'l'}
                                key={platformId}
                                checked={selectedPlatforms.includes(platformId)}
                                onChange={() => handlePlatformFilterChange(platformId)}
                                className={styles.platformCheckbox}
                            >
                                {getPlatformName(platformId)}
                            </Checkbox>
                        ))}
                    </div>
                </div>
            </SidebarMobile>

            {/*Default state*/}
            <div className={cn(styles.searchFilterContainer)}>
                <div ref={searchFilterContainerRef} className={cn(styles.searchFilterContainerWrapper)}>
                    <Container mode="container" className={styles.searchFilterWrapper}>
                        {/*Switcher*/}
                        <div className={styles.modeSwitcher}>
                            <ModeSwitcher
                                value={searchMode}
                                onChange={(value) => {
                                    handleSearchModeChange(value);
                                    trackEvent(GAEvent.SEARCH_MODE_DROPDOWN_CLICK, {})
                                }}
                            />
                        </div>

                        <div className={styles.searchFieldWrapper}>
                            {/*Search input*/}
                            <SearchField
                                value={selectedCategory ? categorySearchQuery : textQuery}
                                onChange={selectedCategory ? onCategorySearch : handleTextQueryChange}
                                onClear={selectedCategory ? onCategorySearchClear : undefined}
                                selectedCategory={selectedCategory}
                                onCategoryReset={onCategoryReset}
                                projectsCount={projectsCount}
                            />

                            {/*Filter area*/}

                            <div className={styles.searchFilterMobileTrigger}>
                                <DropdownTrigger icon={<FilterIcon/>} onClick={handleSidebarOpen}
                                                 counter={selectedPlatforms.length} isMobile/>
                            </div>

                            <div className={styles.searchFilterDropdownTriggers}>
                                <Dropdown
                                    trigger={
                                        <DropdownTrigger
                                            title={'Platforms'}
                                            icon={<ProjectsIcon/>}
                                            onClick={() => {
                                                handlePlatformDropdownClick();
                                                trackEvent(GAEvent.FILTER_DROPDOWN_CLICK, {})
                                            }}
                                            counter={selectedPlatforms.length}
                                            data-testid="platform-filter-dropdown"
                                        />}
                                    isOpen={showPlatformDropdown}
                                    onRequestClose={handleClosePlatformDropdown}
                                >
                                    <PlatformFilter
                                        platforms={filteredPlatforms}
                                        selected={selectedPlatforms}
                                        onToggle={handlePlatformFilterChange}
                                    />
                                </Dropdown>
                            </div>
                        </div>
                    </Container>
                </div>
            </div>


            {/*Compact Filter*/}
            {showCompactFilter && (
                <div className={styles.compactWrapper}>
                    <Container mode="container">
                        <div className={styles.compactInner}>
                            {!selectedCategory && (
                                <div className={styles.compactModeSwitcher}>
                                    <ModeSwitcher
                                        value={searchMode}
                                        onChange={(value) => {
                                            handleSearchModeChange(value);
                                            trackEvent(GAEvent.SEARCH_MODE_DROPDOWN_CLICK, {})
                                        }}
                                    />
                                </div>
                            )}

                            <div className={styles.compactSearchContainer}>
                                <SearchField
                                    value={selectedCategory ? categorySearchQuery : textQuery}
                                    onEnter={selectedCategory ? onCategorySearch : handleTextQueryChange}
                                    onChange={selectedCategory ? onCategorySearch : undefined}
                                    onClear={selectedCategory ? onCategorySearchClear : undefined}
                                    compact
                                    selectedCategory={selectedCategory}
                                    onCategoryReset={onCategoryReset}
                                    projectsCount={projectsCount}
                                />
                                <div className={styles.compactPlatformSelector}>
                                    <Dropdown
                                        trigger={
                                            <DropdownTrigger
                                                compact
                                                icon={<ProjectsIcon/>}
                                                onClick={() => {
                                                    handlePlatformDropdownClick();
                                                    trackEvent(GAEvent.FILTER_DROPDOWN_CLICK, {})
                                                }}
                                                counter={selectedPlatforms.length}
                                                data-testid="platform-filter-dropdown"
                                            />}
                                        isOpen={showPlatformDropdown}
                                        onRequestClose={handleClosePlatformDropdown}
                                    >
                                        <PlatformFilter
                                            platforms={filteredPlatforms}
                                            selected={selectedPlatforms}
                                            onToggle={handlePlatformFilterChange}
                                        />
                                    </Dropdown>
                                </div>

                                <div className={styles.compactMenuTrigger}>
                                    <DropdownTrigger
                                        compact
                                        icon={<FilterIcon/>}
                                        onClick={handleSidebarOpen}
                                        counter={selectedPlatforms.length}
                                    />
                                </div>

                            </div>

                        </div>

                    </Container>
                </div>
            )}
        </>
    );
}

interface PlatformFilterProps {
    platforms: Platform[];
    selected: Platform[];
    onToggle: (platform: Platform) => void;
}

function PlatformFilter({platforms, selected, onToggle}: PlatformFilterProps) {
    return (
        <div className={styles.searchFilterDropdownWrapper}>
            <div className={styles.filterWrapper}>
                {platforms.map((platformId) => (
                    <Checkbox
                        theme="light"
                        mode="rock"
                        key={platformId}
                        checked={selected.includes(platformId)}
                        onChange={() => onToggle(platformId)}
                        className={styles.platformCheckbox}
                    >
                        {getPlatformName(platformId)}
                    </Checkbox>
                ))}
            </div>
        </div>
    );
}

interface ModeSwitcherProps {
    value: SearchMode;
    onChange: (value: SearchMode) => void;
}

function ModeSwitcher({value, onChange}: ModeSwitcherProps) {
    return (
        <Switcher
            value={value}
            onChange={onChange}
            compact
            theme={'dark'}
            mode={'rock'}
            data-e2e="search-by-mode"
            options={[
                {label: 'Projects', value: 'projects'},
                {label: 'Packages', value: 'packages'},
            ]}
        />
    );
}
