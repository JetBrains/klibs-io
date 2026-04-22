"use client"

import React, { useCallback, useEffect, useRef, useState } from "react";
import cn from 'classnames';
import Image from "next/image";
import { Category, ProjectSearchResults } from "@/app/types";
import { searchProjects } from "@/app/api";
import Container from "@/app/ui/container";
import ProjectCard from "@/app/ui/project-card";
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

    useEffect(() => {
        currentPageRef.current = currentPage;
    }, [currentPage]);

    const fetchProjects = useCallback(async (page: number, reset: boolean) => {
        setLoading(true);
        try {
            const results = await searchProjects({
                page,
                limit: SEARCH_LIMIT,
                markers: category.markers,
                query: searchQuery || undefined
            });

            if (reset) {
                setProjects(results);
            } else {
                setProjects(prev => [...prev, ...results]);
            }
            setHasMore(results.length === SEARCH_LIMIT);
        } catch (error) {
            console.error("Error fetching projects:", error);
        } finally {
            setLoading(false);
        }
    }, [category.markers, searchQuery]);

    useEffect(() => {
        setCurrentPage(1);
        fetchProjects(1, true);
    }, [fetchProjects]);

    const handleObserver = useCallback(() => {
        if (!hasMore || loading) return;

        const nextPage = currentPageRef.current + 1;
        setCurrentPage(nextPage);
        fetchProjects(nextPage, false);
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
            <h1 className={cn(textCn("rs-h1"))} style={{ margin: '24px 0' }}>{category.name}</h1>

            {loading && projects.length === 0 ? (
                <div className="row w-100 justify-content-center py-5">
                    <div className="col-md-1 col-3">
                        <KodeeSpinner />
                    </div>
                </div>
            ) : projects.length > 0 ? (
                <Container mode="wrapper" cardGrid>
                    {projects.map((project) => (
                        <ProjectCard key={project.id} featuredProject={project} />
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
