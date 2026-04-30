"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import cn from "classnames";

import { tableCn } from "@rescui/table";
import { textCn } from "@rescui/typography";

import Container from "@/app/ui/container";
import KodeeSpinner from "@/app/ui/kodee-spinner";
import { searchProjects } from "@/app/api";
import { getProjectLink, ProjectSearchResults } from "@/app/types";

import styles from "./styles.module.css";

const TOP_LIMIT = 50;

export default function InsightsPage() {
    const [healthy, setHealthy] = useState<ProjectSearchResults[] | null>(null);
    const [dependents, setDependents] = useState<ProjectSearchResults[] | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        Promise.all([
            searchProjects({ sort: 'most-healthy', page: 1, limit: TOP_LIMIT }),
            searchProjects({ sort: 'most-dependents', page: 1, limit: TOP_LIMIT }),
        ])
            .then(([byHealth, byDependents]) => {
                if (cancelled) return;
                setHealthy(byHealth);
                setDependents(byDependents);
            })
            .catch((e) => {
                if (cancelled) return;
                setError(e instanceof Error ? e.message : String(e));
            });
        return () => {
            cancelled = true;
        };
    }, []);

    const isLoading = !error && (healthy === null || dependents === null);

    return (
        <Container mode="container" className="padding-top-medium padding-bottom-large">
            <Container mode="wrapper">
                <h1 className={cn(textCn('rs-h1'), 'padding-top-medium')}>Insights</h1>
                <p className={cn(textCn('rs-text-2', { hardness: 'hard' }), styles.intro)}>
                    Internal leaderboards. Not linked from anywhere; intended for ad-hoc inspection.
                </p>

                {error && (
                    <p className={textCn('rs-text-2')} role="alert">
                        Failed to load insights: {error}
                    </p>
                )}

                {isLoading && (
                    <div className={styles.spinnerWrapper}>
                        <KodeeSpinner />
                    </div>
                )}

                {!isLoading && !error && (
                    <>
                        <Section title="Most healthy projects" subtitle={`Top ${TOP_LIMIT} by OSS Health Index (0-100, highest first).`}>
                            <ProjectsTable projects={healthy ?? []} highlight="health" />
                        </Section>

                        <Section title="Projects with the most dependents" subtitle={`Top ${TOP_LIMIT} by number of distinct projects that depend on this project's packages.`}>
                            <ProjectsTable projects={dependents ?? []} highlight="dependents" />
                        </Section>

                        <Section title="Packages with the most dependents" subtitle="Not implemented — backend does not expose per-package dependent counts. Tracked as TODO.">
                            <p className={textCn('rs-text-2', { hardness: 'hard' })}>
                                The backend currently aggregates dependents on the project level only.
                                A per-package or per-(groupId, artifactId) leaderboard requires a new endpoint over <code>package_dependency</code>.
                            </p>
                        </Section>
                    </>
                )}
            </Container>
        </Container>
    );
}

function Section({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
    return (
        <section className={styles.section}>
            <h2 className={cn(textCn('rs-h2'), styles.sectionTitle)}>{title}</h2>
            {subtitle && <p className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.sectionSubtitle)}>{subtitle}</p>}
            {children}
        </section>
    );
}

interface ProjectsTableProps {
    projects: ProjectSearchResults[];
    highlight: 'health' | 'dependents';
}

function ProjectsTable({ projects, highlight }: ProjectsTableProps) {
    if (projects.length === 0) {
        return <p className={textCn('rs-text-2', { hardness: 'hard' })}>No data.</p>;
    }

    return (
        <div className={styles.tableWrapper}>
            <table className={cn(tableCn({ isWide: true, size: 'm' }), styles.table)}>
                <thead>
                    <tr className={textCn('rs-text-3', { hardness: 'hard' })}>
                        <th className={styles.rankCol}>#</th>
                        <th>Project</th>
                        <th className={styles.numericCol}>Stars</th>
                        <th className={styles.numericCol}>
                            {highlight === 'dependents' ? 'Dependents ↓' : 'Dependents'}
                        </th>
                        <th className={styles.numericCol}>
                            {highlight === 'health' ? 'Health ↓' : 'Health'}
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {projects.map((project, idx) => (
                        <tr key={project.id} className={cn('align-middle', textCn('rs-text-3'))}>
                            <td className={styles.rankCol}>{idx + 1}</td>
                            <td>
                                <Link href={getProjectLink(project)} className={textCn('rs-link')}>
                                    {project.ownerLogin}/{project.name}
                                </Link>
                            </td>
                            <td className={styles.numericCol}>{project.scmStars}</td>
                            <td className={styles.numericCol}>{project.dependentCount}</td>
                            <td className={styles.numericCol}>{project.ossHealthScore ?? '—'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
