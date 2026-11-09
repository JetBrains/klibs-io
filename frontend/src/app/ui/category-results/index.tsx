"use client"

import React, { useCallback, useEffect, useRef, useState } from "react";
import cn from 'classnames';
import Image from "next/image";
import { Category, ProjectSearchResults } from "@/app/types";
import { searchProjects } from "@/app/api";
import Container from "@/app/ui/container";
import ProjectCardHorizontal from "@/app/ui/project-card-horizontal";
import KodeeSpinner from "@/app/ui/kodee-spinner";
import KodeeSad from "@/app/img/kodee/kodee-sad.png";
import { textCn } from "@rescui/typography";

const SEARCH_LIMIT = 15;

interface CategoryResultsProps {
    category: Category;
    searchQuery?: string;
}

export default function CategoryResults({ category, searchQuery }: CategoryResultsProps) {
    const [projects, setProjects] = useState<ProjectSearchResults[]>([]);
    const [loading, setLoading] = useState(true);
    const [hasMore, setHasMore] = useState(false);
    const [currentPage, setCurrentPage] = useState(1);
    const currentPageRef = useRef(currentPage);
    const loadMoreRef = useRef<HTMLDivElement | null>(null);
    const requestGeneration = useRef(0);

    useEffect(() => {
        currentPageRef.current = currentPage;
    }, [currentPage]);

    const fetchProjects = useCallback(async (page: number, reset: boolean, generation: number) => {
        setLoading(true);
        try {
            const results = await searchProjects({
                page,
                limit: SEARCH_LIMIT,
                markers: category.markers,
                sort: category.markers.includes('JetBrains and Google') ? 'most-dependents' : undefined,
                query: searchQuery || undefined
            });

            if (generation !== requestGeneration.current) return;
            if (reset) {
                setProjects(results);
            } else {
                setProjects(prev => [...prev, ...results]);
            }
            setHasMore(results.length === SEARCH_LIMIT);
        } catch (error) {
            console.error("Error fetching projects:", error);
        } finally {
            if (generation === requestGeneration.current) setLoading(false);
        }
    }, [category.markers, searchQuery]);

    useEffect(() => {
        const generation = ++requestGeneration.current;
        setCurrentPage(1);
        if (category.markers.length === 0) {
            setProjects([]);
            setHasMore(false);
            setLoading(false);
        } else {
            fetchProjects(1, true, generation);
        }
        return () => { requestGeneration.current++; };
    }, [fetchProjects, category.markers]);

    const handleObserver = useCallback(() => {
        if (!hasMore || loading) return;

        const nextPage = currentPageRef.current + 1;
        setCurrentPage(nextPage);
        fetchProjects(nextPage, false, requestGeneration.current);
    }, [hasMore, loading, fetchProjects]);

    useEffect(() => {
        const refElement = loadMoreRef.current;
        if (!refElement || !projects.length) return;

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
    }, [handleObserver, projects]);

    return (
        <>
            <div style={{ margin: '24px 0' }}>
                <h1 className={cn(textCn("rs-h1"))} style={{ margin: 0 }}>{category.name}</h1>
                {category.subtitle && (
                    <p className={textCn("rs-text-3")} style={{ margin: '8px 0 0' }}>
                        {category.subtitle}
                    </p>
                )}
            </div>

            {loading && projects.length === 0 ? (
                <div className="row w-100 justify-content-center py-5">
                    <div className="col-md-1 col-3">
                        <KodeeSpinner />
                    </div>
                </div>
            ) : projects.length > 0 ? (
                <Container mode="wrapper" cardColumn>
                    {projects.map((project) => (
                        <ProjectCardHorizontal key={project.id} project={project} search={searchQuery} />
                    ))}
                    <div ref={loadMoreRef}></div>
                </Container>
            ) : (
                <div className="row w-100 justify-content-center text-center py-5">
                    <div className="col-md-2 col-6">
                        <Image src={KodeeSad} alt={"Sad Kodee"} className="rounded img-fluid" />
                    </div>
                    <div className="col-auto align-content-center order-md-last order-first pb-md-0 pb-5">
                        <h4>{searchQuery ? `No projects found for "${searchQuery}" in this category` : "No projects found in this category"}</h4>
                    </div>
                </div>
            )}
        </>
    );
}
