"use client"

import cn from 'classnames';
import {usePathname, useRouter} from 'next/navigation';
import klibsLogo from '@/app/img/klibs-logo.svg';
import Image from "next/image";
import Link from "next/link";

import styles from './styles.module.css';
import {textCn} from '@rescui/typography';
import SearchField from "@/app/ui/search-field";
import React, {useCallback, useState, useEffect} from "react";
import Container from "@/app/ui/container";
import {KotlinIcon, HamburgerIcon, SlackIcon, MessageIcon, InfoIcon} from "@rescui/icons";
import {MenuItem} from "@rescui/menu";
import {Button} from "@rescui/button";

import {DropdownMenu} from "@rescui/dropdown-menu";

import {ProjectSearchResults} from "@/app/types";

import {searchProjects} from "@/app/api";


import {SidebarMenuHeader} from '@jetbrains/kotlin-web-site-ui/out/components/sidebar-menu';
import {Sidebar} from '@jetbrains/kotlin-web-site-ui/out/components/sidebar';

import '@jetbrains/kotlin-web-site-ui/out/components/typography';
import {ThemeProvider} from '@rescui/ui-contexts';

import {KotlinEcosystemDropdown} from "@/app/ui/kotlin-ecosystem-dropdown/kotlin-ecosystem-dropdown";
import {KotlinEcosystemMobileMenu} from "@/app/ui/kotlin-ecosystem-dropdown/kotlin-ecosystem-mobile-menu";

import {trackEvent, GAEvent} from "@/app/analytics";

const ISSUES_LINK = "https://github.com/JetBrains/klibs-io-issue-management/issues/new/choose"


export default function Navbar() {
    const pathname = usePathname();
    const isIndex = pathname === '/';

    const isSearchPage = pathname === '/search';
    const router = useRouter();

    const onEnter = useCallback((inputValue: string) => {
        router.push(`/?query=${encodeURIComponent(inputValue)}`);
    }, [router]);

    // Dropdown menu constants
    const [isOpen, setIsOpen] = useState(false);
    const toggleIsOpen = () => setIsOpen(s => !s);

    // Suggestions. For now we're using search query with limit
    const [searchSuggestions, setSearchSuggestions] = useState<ProjectSearchResults[] | null>(null)

    const handleNavbarSearchInput = useCallback(async (value: string) => {
        const result = await searchProjects({query: value, limit: 5, page: 1});
        setSearchSuggestions(result);
    }, [])

    const handleSuggestionsClose = useCallback(() => {
        setSearchSuggestions(null);
    }, [])

    const [mobileMenuVisible, setMobileMenuVisible] = useState<boolean>(false);
    const [kotlinEcosystemMobileMenuVisible, setKotlinEcosystemMobileMenuVisible] = useState<boolean>(false);

    const handleSidebarOpen = () => {
        setKotlinEcosystemMobileMenuVisible(false);
        setMobileMenuVisible(true);
    }

    const handleSidebarClose = () => {
        setMobileMenuVisible(false);
    }

    useEffect(() => {
        if (mobileMenuVisible) {
            document.documentElement.classList.add('scroll-lock');
        } else {
            document.documentElement.classList.remove('scroll-lock');
        }

        return () => {
            document.documentElement.classList.remove('scroll-lock');
        };
    }, [mobileMenuVisible]);

    return (
        <div className={cn(styles.navbarWrapper, {
            [styles.navbarWrapperInnerPages]: !isIndex,
        })}>
            <nav aria-label="main navigation" className={cn(styles.navbar, {
                [styles.navbarSticky]: !isIndex,
            })}>
                <Container mode={"container"} row nav className={styles.navbarInner}>

                    {/*TODO: Reuse SidebarMobile component*/}
                    <ThemeProvider theme={'dark'}>
                        <Sidebar
                            isOpen={mobileMenuVisible}
                            onClose={handleSidebarClose}
                        >
                            <div className={styles.focusTrapWrapper}>
                                {kotlinEcosystemMobileMenuVisible ? (
                                    <>
                                        <SidebarMenuHeader>
                                            <div className={textCn('rs-text-2')}>Kotlin ecosystem</div>
                                        </SidebarMenuHeader>
                                        <KotlinEcosystemMobileMenu/>
                                    </>
                                ) : <>
                                    <SidebarMenuHeader>
                                        <div className={textCn('rs-text-2')}>klibs.io</div>
                                    </SidebarMenuHeader>
                                    <div>
                                        <MenuItem
                                            href={ISSUES_LINK}
                                            onClick={() =>
                                                trackEvent(GAEvent.REPORT_AN_ISSUE_CLICK, {})
                                            }
                                            target={"_blank"} size={"l"} icon={<MessageIcon/>}>Report an
                                            issue</MenuItem>
                                        <MenuItem onClick={() =>
                                            trackEvent(GAEvent.FAQ_CLICK, {})
                                        } href={"/faq"} size={"l"} icon={<InfoIcon/>}>About & FAQ</MenuItem>
                                        <MenuItem size={"l"} icon={<KotlinIcon/>}
                                                  onClick={() => setKotlinEcosystemMobileMenuVisible(true)}>Kotlin
                                            Ecosystem</MenuItem>
                                    </div>
                                </>}
                            </div>
                        </Sidebar>
                    </ThemeProvider>

                    {/*Logo*/}
                    {/* intentionally not using Link to easily refresh the index page */}
                    <a
                        href="/"
                        className={cn(styles.navItem, styles.logoWrapper)}
                        onClick={() => {
                            trackEvent(GAEvent.LOGO_CLICK, {})
                        }}
                    >
                        <Image className={styles.navbarLogo} src={klibsLogo} priority alt="Klibs.io wordmark"/>
                    </a>

                    {/*Main page nav */}
                    {(isIndex || isSearchPage) &&
                        <div className={cn(styles.navigation, textCn('rs-text-1'))}>
                            <a
                                href={ISSUES_LINK}
                                target="_blank"
                                className={cn(textCn('rs-link', {mode: 'clear'}), styles.navItem, "hide-on-small")}
                                onClick={() => trackEvent(GAEvent.REPORT_AN_ISSUE_CLICK, {})}
                            >
                                Report an issue
                            </a>

                            <Link
                                href="/faq"
                                className={cn(textCn('rs-link', {mode: 'clear'}), styles.navItem, "hide-on-small")}
                                onClick={() => trackEvent(GAEvent.FAQ_CLICK, {})}
                            >
                                FAQ
                            </Link>

                            <Link
                                href="/faq#slack-guide"
                                data-testid="slack-link"
                                className={cn(textCn('rs-link', {mode: 'clear'}), styles.navItem, styles.navIcon, "hide-on-small")}
                                onClick={() => trackEvent(GAEvent.SLACK_CLICK, {})}
                            >
                                <SlackIcon/>
                            </Link>

                            <Button
                                className={cn(styles.navItem, styles.menuButton)}
                                onClick={handleSidebarOpen}
                                content={"icon"}
                                icon={<HamburgerIcon/>}
                                mode={"clear"}
                                data-testid="mobile-menu-button"
                            />

                            <KotlinEcosystemDropdown/>
                        </div>
                    }

                    {/*Project page nav with search*/}
                    {(!isIndex && !isSearchPage) &&
                        <>
                            <div className={cn(styles.navigation, textCn('rs-text-2'))}>
                                {/*Search input*/}
                                <SearchField
                                    onEnter={onEnter}
                                    onChange={handleNavbarSearchInput}
                                    value={""}
                                    suggestionsList={searchSuggestions}
                                    suggestionsClose={handleSuggestionsClose}
                                    className={cn("d-none d-md-flex mr-16", styles.searchWrapper)}
                                />
                                {/*Desktop Trigger*/}
                                <DropdownMenu
                                    isOpen={isOpen}
                                    onRequestClose={() => setIsOpen(false)}
                                    size={"l"}
                                    trigger={
                                        <Button
                                            className={cn(styles.navItem, styles.navIcon, styles.menuButton, styles.desktopDropdownTrigger)}
                                            onClick={toggleIsOpen}
                                            content={"icon"}
                                            icon={<HamburgerIcon/>}
                                            mode={"clear"}
                                        />
                                    }
                                >
                                    <MenuItem
                                        href={ISSUES_LINK}
                                        target="_blank"
                                        onClick={() => trackEvent(GAEvent.REPORT_AN_ISSUE_CLICK, {})}
                                    >
                                        Report an issue
                                    </MenuItem>

                                    <MenuItem
                                        href="/faq"
                                        onClick={() => trackEvent(GAEvent.FAQ_CLICK, {})}
                                    >
                                        FAQ
                                    </MenuItem>

                                    <MenuItem
                                        href="/faq#slack-guide"
                                        onClick={() => trackEvent(GAEvent.SLACK_CLICK, {})}
                                    >
                                        Join Slack channel
                                    </MenuItem>
                                </DropdownMenu>

                                {/*Mobile Trigger*/}
                                <Button
                                    className={cn(styles.navItem, styles.menuButton)}
                                    onClick={handleSidebarOpen}
                                    content={"icon"}
                                    icon={<HamburgerIcon/>}
                                    mode={"clear"}
                                />

                                <KotlinEcosystemDropdown/>
                            </div>
                        </>
                    }
                </Container>
            </nav>
        </div>
    );
}

