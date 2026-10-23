'use client'

import {ReactNode, useEffect, useMemo, useState} from "react";
import Link from "next/link";
import {useSearchParams} from "next/navigation";
import cn from "classnames";
import {Chip, ChipListMulti} from "@rescui/chip-list";
import FocusManager from "@rescui/focus-manager";
import {tableCn, tableOverflowWrapperCn} from "@rescui/table";
import {textCn} from "@rescui/typography";

import {parseCompareParams} from "@/app/helpers";
import {CompareProjectResponse, getTargetGroupNames, kFormatter} from "@/app/types";
import {useCompare} from "@/app/ui/compare-provider";
import Container from "@/app/ui/container";
import {ContentFailed} from "@/app/ui/content-failed";
import PlatformTag from "@/app/ui/platform-tag";
import TimeAgo from "@/app/ui/time-ago";

import styles from './styles.module.css';

interface ComparePageContentProps {
    projects: CompareProjectResponse[] | null;
}

interface CompareRow {
    label: string;
    render: (project: CompareProjectResponse) => ReactNode;
    // `now` is null until mount, so time-based checks don't differ between server and client
    isGood?: (project: CompareProjectResponse, now: number | null) => boolean;
}

const DESCRIPTION_MAX_LENGTH = 300;

const truncate = (text: string | null, maxLength: number) =>
    text && text.length > maxLength ? `${text.slice(0, maxLength).trimEnd()}…` : text;

// SPDX ids plus the GitHub display names that licenseName actually carries
const PERMISSIVE_LICENSES = new Set([
    'MIT', 'Apache-2.0', 'ISC', 'BSD-2-Clause', 'BSD-3-Clause', 'Unlicense', '0BSD',
    'MIT License', 'Apache License 2.0', 'ISC License', 'BSD 2-Clause "Simplified" License',
    'BSD 3-Clause "New" or "Revised" License', 'The Unlicense', 'BSD Zero Clause License',
]);

const FRESH_PERIOD_MILLIS = 3 * 30 * 24 * 60 * 60 * 1000;

const isRecent = (timestamp: number, now: number | null) => now !== null && now - timestamp < FRESH_PERIOD_MILLIS;

const ROWS: CompareRow[] = [
    {label: 'Description', render: project => truncate(project.description, DESCRIPTION_MAX_LENGTH)},
    {label: 'Stars', render: project => kFormatter(project.scmStars)},
    {
        label: 'License', render: project => project.licenseName,
        isGood: project => project.licenseName !== null && PERMISSIVE_LICENSES.has(project.licenseName),
    },
    {label: 'Latest release', render: project => project.latestReleasePublishedAtMillis &&
        <TimeAgo timestamp={project.latestReleasePublishedAtMillis} strictMode/>},
    {
        label: 'Kotlin version', render: project => project.kotlinVersion,
        isGood: project => project.kotlinVersion?.startsWith('2.') ?? false,
    },
    {label: 'Platforms', render: project => <div className={styles.platforms}>
        {getTargetGroupNames(project.targetGroups).map(group => <PlatformTag key={group}>{group}</PlatformTag>)}
    </div>},
    {label: 'Open issues', render: project => project.openIssues},
    {
        label: 'Last activity', render: project => <TimeAgo timestamp={project.lastActivityAtMillis} strictMode/>,
        isGood: (project, now) => isRecent(project.lastActivityAtMillis, now),
    },
    {label: 'Created', render: project => <TimeAgo timestamp={project.createdAtMillis} strictMode/>},
    {label: 'Tags', render: project => project.tags.length > 0 ? project.tags.map(tag => `#${tag}`).join(', ') : null},
];

function CompareTable({projects, rows}: { projects: CompareProjectResponse[], rows: CompareRow[] }) {
    const [now, setNow] = useState<number | null>(null);
    useEffect(() => setNow(Date.now()), []);

    return (
        <div className={tableOverflowWrapperCn()}>
            <table className={cn(tableCn({isWide: true, size: 's'}), styles.table)}>
                <thead>
                    <tr>
                        <th scope="col" className={styles.labelColumn}/>
                        {projects.map(project => (
                            <th scope="col" key={`${project.ownerLogin}/${project.projectName}`}>
                                <Link href={`/project/${project.ownerLogin}/${project.projectName}`}
                                      className={textCn('rs-h5')}>
                                    {project.projectName}
                                </Link>
                                <div className={textCn('rs-text-3')}>by {project.ownerLogin}</div>
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {rows.map(row => (
                        <tr key={row.label}>
                            <td className={cn(textCn('rs-text-3', {hardness: 'hard'}), styles.labelColumn)}>
                                {row.label}
                            </td>
                            {projects.map(project => (
                                <td key={`${project.ownerLogin}/${project.projectName}`}
                                    className={cn({[styles.status_good]: row.isGood?.(project, now)})}>
                                    {row.render(project) ?? '—'}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

// Follows the current URL, so removals from the compare bar apply without refetching
function useComparedProjects(projects: CompareProjectResponse[] | null) {
    const searchParams = useSearchParams();
    return useMemo(() => {
        if (!projects || !searchParams) return projects;
        const references = parseCompareParams(searchParams.getAll('projects'));
        return projects.filter(project => references.some(
            ref => ref.ownerLogin === project.ownerLogin && ref.projectName === project.projectName
        ));
    }, [projects, searchParams]);
}

export default function ComparePageContent(props: ComparePageContentProps) {
    const projects = useComparedProjects(props.projects);
    const [visibleLabels, setVisibleLabels] = useState<string[]>(ROWS.map(row => row.label));
    const replaceCompared = useCompare()?.replace;

    useEffect(() => {
        const focusManager = new FocusManager();
        return () => focusManager.destroy();
    }, []);

    // Syncs the compare bar with the URL, so a reload or link restores it and an empty URL clears it
    useEffect(() => {
        if (projects) replaceCompared?.(projects.map(it => ({ownerLogin: it.ownerLogin, name: it.projectName})));
    }, [projects, replaceCompared]);

    if (projects === null) return <ContentFailed/>;

    return (
        <Container mode="container" className={styles.pageContainer}>
            <Container mode="wrapper">
                <h1 className={cn(textCn('rs-h1'), styles.title)}>Compare projects</h1>
                {projects.length > 0 ? <>
                    <ChipListMulti value={visibleLabels} onChange={setVisibleLabels} mode="rock" size="s" compact
                                   className={styles.rowFilter}>
                        {ROWS.map(row => <Chip key={row.label} value={row.label}>{row.label}</Chip>)}
                    </ChipListMulti>
                    {visibleLabels.length > 0 ?
                        <CompareTable projects={projects} rows={ROWS.filter(row => visibleLabels.includes(row.label))}/> :
                        <p className={textCn('rs-text-3', {hardness: 'hard'})}>
                            No properties selected. Pick the ones you want to compare.
                        </p>
                    }
                </> :
                    <p className={textCn('rs-text-2', {hardness: 'hard'})}>
                        Nothing to compare yet. Use the Compare toggle on project cards
                        to <Link href="/">pick projects</Link>.
                    </p>
                }
            </Container>
        </Container>
    );
}
