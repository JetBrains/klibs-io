import Image from "next/image";
import Link from 'next/link';
import cn from 'classnames';
import ProjectCard from "@/app/ui/project-card";
import PackageCard from "@/app/ui/package-card";
import KodeeSpinner from "@/app/ui/kodee-spinner";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { PackageSearchResults, ProjectSearchResults, SearchParams } from "@/app/types";
import { searchProjects, searchPackages } from "@/app/api";
import Container from "@/app/ui/container";

import KodeeNotFound from '@/app/img/kodee/kodee-404.svg'

import { textCn } from '@rescui/typography'

interface SearchResultsProps {
    filters: SearchParams;
    isPackageSearch: boolean;
}

const SEARCH_LIMIT = 18;

export function SearchResults({ filters, isPackageSearch }: SearchResultsProps) {
    const [projects, setProjects] = useState<ProjectSearchResults[] | null>(null);
    const [packages, setPackages] = useState<PackageSearchResults[] | null>(null);
    const [loading, setLoading] = useState(true);
    const [hasMore, setHasMore] = useState(false);
    const [currentPage, setPage] = useState(filters.page);
    const currentPageRef = useRef(currentPage);
    const loadMoreRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        currentPageRef.current = currentPage;
    }, [currentPage]);

    const fetchItems = useCallback(async ({ page = 1, resetItems = false, filters }: { page: number; resetItems: boolean; filters: SearchParams }) => {
        setLoading(true);

        try {
            if (isPackageSearch) {
                const newPackages = await searchPackages({
                    ...filters,
                    page: page,
                    limit: SEARCH_LIMIT
                });

                if (resetItems) {
                    setPackages(newPackages);
                } else {
                    setPackages(prevItems => [...(prevItems || []), ...newPackages]);
                }

                setHasMore(newPackages.length == SEARCH_LIMIT);
            } else {
                const newProjects = await searchProjects({
                    ...filters,
                    page: page,
                    limit: SEARCH_LIMIT
                });

                if (resetItems) {
                    setProjects(newProjects);
                } else {
                    setProjects(prevItems => [...(prevItems || []), ...newProjects]);
                }

                setHasMore(newProjects.length == SEARCH_LIMIT);
            }
        } catch (error) {
            console.error("Error fetching items:", error);
        } finally {
            setLoading(false);
        }
    }, [isPackageSearch]);

    useEffect(() => {
        const page = 1;
        setPage(page);
        fetchItems({ page: page, resetItems: true, filters });
    }, [fetchItems, filters]);

    const handleObserver = useCallback(() => {
        if (!hasMore) {
            return;
        }

        const nextPage = currentPageRef.current + 1;
        setPage(nextPage);

        fetchItems({
            page: nextPage,
            resetItems: false,
            filters
        });
    }, [hasMore, fetchItems, filters]);

    useEffect(() => {
        const refElement = loadMoreRef.current;
        if (!refElement || !(isPackageSearch ? packages?.length : projects?.length)) return;

        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                handleObserver();
            }
        }, {
            root: null,
            rootMargin: "0px",
            threshold: 1.0,
        });

        observer.observe(refElement);

        return () => {
            if (observer && refElement) {
                observer.unobserve(refElement);
                observer.disconnect();
            }
        };
    }, [handleObserver, projects, packages, isPackageSearch]);

    if (loading && !hasMore) {
        return (
            <div id="request-indicator-spinner"
                className="row w-100 justify-content-center py-5">
                <div className="col-md-1 col-3">
                    <KodeeSpinner />
                </div>
            </div>
        );
    } else if (packages?.length || projects?.length) {
        return (
            <Container mode="wrapper" cardGrid>
                {/*Search results cards*/}
                {isPackageSearch
                    ? packages?.map((item) => (
                        <PackageCard search={filters?.query} featuredPackage={item} key={item.id} />
                    ))
                    : projects?.map((item) => (
                        <ProjectCard search={filters?.query} featuredProject={item} key={item.id} />
                    ))
                }

                <div ref={loadMoreRef}></div>
            </Container>
        );
    } else {
        return (
            <div className="row w-100 py-5 flex-column-reverse flex-md-row">
                <p className="col-md-4 d-flex justify-content-center">
                    <Image src={KodeeNotFound} alt={"Kodee 404"} className="rounded img-fluid" />
                </p>

                <div className="col-md-8 d-flex flex-column">
                    <h2 className={textCn('rs-h2')} data-testid="search-no-results-message">I couldn&apos;t find anything for your search.</h2>

                    <br />

                    <p className={textCn('rs-text-2', { hardness: 'hard' })}>Don&apos;t worry — here are some useful links to get you started.</p>

                    <ul className={cn(textCn('rs-ul'), textCn('rs-text-2'))}>
                        <li>
                            <p className={textCn('rs-text-2', { hardness: 'hard' })}>Explore official <Link href={'/organization/JetBrains'} className={textCn('rs-link')}>JetBrains libraries</Link></p>
                        </li>
                        <li><Link href={'https://kotlinlang.org/docs/api-guidelines-build-for-multiplatform.html'} className={textCn('rs-link', { external: true })}>Learn how to build a Kotlin Multiplatform library</Link></li>
                        <li><Link href={'https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html'} className={textCn('rs-link', { external: true })}>Find out how to publish your library</Link></li>
                        <li><Link href={'/faq#how-do-i-add-a-project'} className={textCn('rs-link')}>Get your project listed on klibs.io</Link></li>
                    </ul>
                </div>
            </div>
        );
    }
}
