"use client";

import React, { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import cn from "classnames";

import { Select } from "@rescui/select";
import { tableCn } from "@rescui/table";
import { textCn } from "@rescui/typography";

import Container from "@/app/ui/container";
import KodeeSpinner from "@/app/ui/kodee-spinner";
import { searchProjects } from "@/app/api";
import { getProjectLink, ProjectSearchResults, SearchSort } from "@/app/types";

import styles from "./styles.module.css";

const PAGE_SIZE = 50;

type InsightsSort = Extract<SearchSort, "most-dependents" | "most-healthy" | "most-stars">;

const SORT_OPTIONS: { value: InsightsSort; label: string }[] = [
    { value: "most-dependents", label: "Most dependents" },
    { value: "most-healthy", label: "Most healthy" },
    { value: "most-stars", label: "Most stars" },
];

const DEFAULT_SORT: InsightsSort = "most-dependents";

export default function InsightsPage() {
    const [sort, setSort] = useState<InsightsSort>(DEFAULT_SORT);
    const [projects, setProjects] = useState<ProjectSearchResults[]>([]);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const pageRef = useRef(page);
    const loadMoreRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        pageRef.current = page;
    }, [page]);

    const fetchPage = useCallback(
        async ({ sort, page, append }: { sort: InsightsSort; page: number; append: boolean }) => {
            setLoading(true);
            setError(null);
            try {
                const next = await searchProjects({ sort, page, limit: PAGE_SIZE });
                setProjects((prev) => (append ? [...prev, ...next] : next));
                setHasMore(next.length === PAGE_SIZE);
            } catch (e) {
                setError(e instanceof Error ? e.message : String(e));
                setHasMore(false);
            } finally {
                setLoading(false);
            }
        },
        []
    );

    useEffect(() => {
        setPage(1);
        fetchPage({ sort, page: 1, append: false });
    }, [sort, fetchPage]);

    const loadNextPage = useCallback(() => {
        if (!hasMore || loading) return;
        const nextPage = pageRef.current + 1;
        setPage(nextPage);
        fetchPage({ sort, page: nextPage, append: true });
    }, [hasMore, loading, sort, fetchPage]);

    useEffect(() => {
        const ref = loadMoreRef.current;
        if (!ref || projects.length === 0) return;

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    loadNextPage();
                }
            },
            { root: null, rootMargin: "0px", threshold: 1.0 }
        );

        observer.observe(ref);
        return () => observer.disconnect();
    }, [loadNextPage, projects.length]);

    const currentOption = SORT_OPTIONS.find((o) => o.value === sort) ?? SORT_OPTIONS[0];

    return (
        <Container mode="container" className="padding-top-medium padding-bottom-large">
            <Container mode="wrapper">
                <h1 className={cn(textCn("rs-h1"), "padding-top-medium")}>Insights</h1>
                <p className={cn(textCn("rs-text-2", { hardness: "hard" }), styles.intro)}>
                    Internal leaderboard. Not linked from anywhere; intended for ad-hoc inspection.
                </p>

                <div className={styles.controls}>
                    <span className={cn(textCn("rs-text-3", { hardness: "hard" }), styles.controlsLabel)}>
                        Sort by:
                    </span>
                    <Select
                        className={styles.controlsSelect}
                        mode="rock"
                        size="m"
                        options={SORT_OPTIONS}
                        value={currentOption}
                        onChange={(option: { label: string; value?: string }) => {
                            const next = option.value as InsightsSort;
                            if (next !== sort) setSort(next);
                        }}
                    />
                </div>

                {error && (
                    <p className={textCn("rs-text-2")} role="alert">
                        Failed to load insights: {error}
                    </p>
                )}

                {projects.length > 0 && (
                    <ProjectsTable projects={projects} highlight={sort} />
                )}

                {projects.length === 0 && !loading && !error && (
                    <p className={textCn("rs-text-2", { hardness: "hard" })}>No data.</p>
                )}

                {loading && (
                    <div className={styles.spinnerWrapper}>
                        <KodeeSpinner />
                    </div>
                )}

                <div ref={loadMoreRef} />
            </Container>
        </Container>
    );
}

interface ProjectsTableProps {
    projects: ProjectSearchResults[];
    highlight: InsightsSort;
}

function ProjectsTable({ projects, highlight }: ProjectsTableProps) {
    return (
        <div className={styles.tableWrapper}>
            <table className={cn(tableCn({ isWide: true, size: "m" }), styles.table)}>
                <thead>
                    <tr className={textCn("rs-text-3", { hardness: "hard" })}>
                        <th className={styles.rankCol}>#</th>
                        <th>Project</th>
                        <th className={styles.numericCol}>
                            {highlight === "most-stars" ? "Stars ↓" : "Stars"}
                        </th>
                        <th className={styles.numericCol}>
                            {highlight === "most-dependents" ? "Dependents ↓" : "Dependents"}
                        </th>
                        <th className={styles.numericCol}>
                            {highlight === "most-healthy" ? "Health ↓" : "Health"}
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {projects.map((project, idx) => (
                        <tr key={project.id} className={cn("align-middle", textCn("rs-text-3"))}>
                            <td className={styles.rankCol}>{idx + 1}</td>
                            <td>
                                <Link href={getProjectLink(project)} className={textCn("rs-link")}>
                                    {project.ownerLogin}/{project.name}
                                </Link>
                            </td>
                            <td className={styles.numericCol}>{project.scmStars}</td>
                            <td className={styles.numericCol}>{project.dependentCount}</td>
                            <td className={styles.numericCol}>
                                {project.ossHealthScore ?? "—"}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
