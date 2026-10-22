'use client'

import {ReactNode, useEffect, useMemo, useState} from "react";
import Link from "next/link";
import {useSearchParams} from "next/navigation";
import cn from "classnames";
import {Chip, ChipListMulti} from "@rescui/chip-list";
import FocusManager from "@rescui/focus-manager";
import {tableCn, tableOverflowWrapperCn} from "@rescui/table";
import {Tag} from "@rescui/tag";
import {textCn} from "@rescui/typography";

import {parseCompareParams} from "@/app/helpers";
import {CompareProjectResponse, getPlatformName, kFormatter, Platform, platformOrder} from "@/app/types";
import {useCompare} from "@/app/ui/compare-provider";
import Container from "@/app/ui/container";
import {ContentFailed} from "@/app/ui/content-failed";
import {TimestampToDate} from "@/app/ui/time-ago";

import styles from './styles.module.css';

interface ComparePageContentProps {
    projects: CompareProjectResponse[] | null;
}

interface CompareRow {
    label: string;
    render: (project: CompareProjectResponse) => ReactNode;
}

const sortedPlatforms = (platforms: string[]) =>
    platformOrder.filter(platform => platforms.includes(platform));

// Same platform colors as the project page
const PLATFORM_COLORS: Record<Platform, string> = {
    [Platform.androidJvm]: 'var(--jvm)',
    [Platform.jvm]: 'var(--jvm)',
    [Platform.native]: 'var(--kotlin-native)',
    [Platform.wasm]: 'var(--wasm)',
    [Platform.js]: 'var(--js)',
    [Platform.common]: 'var(--common)',
};

const DESCRIPTION_MAX_LENGTH = 300;

const truncate = (text: string | null, maxLength: number) =>
    text && text.length > maxLength ? `${text.slice(0, maxLength).trimEnd()}…` : text;

const ROWS: CompareRow[] = [
    {label: 'Description', render: project => truncate(project.description, DESCRIPTION_MAX_LENGTH)},
    {label: 'Stars', render: project => kFormatter(project.scmStars)},
    {label: 'License', render: project => project.licenseName},
    {label: 'Latest release', render: project => project.latestReleaseVersion && <>
        {project.latestReleaseVersion}
        {project.latestReleasePublishedAtMillis && <div className={textCn('rs-text-3')}>
            <TimestampToDate timestamp={project.latestReleasePublishedAtMillis}/>
        </div>}
    </>},
    {label: 'Kotlin version', render: project => project.kotlinVersion},
    {label: 'Platforms', render: project => <div className={styles.platforms}>
        {sortedPlatforms(project.platforms).map((platform: Platform) =>
            <Tag key={platform} backgroundColor={PLATFORM_COLORS[platform]}>{getPlatformName(platform)}</Tag>
        )}
    </div>},
    {label: 'Open issues', render: project => project.openIssues},
    {label: 'Last activity', render: project => <TimestampToDate timestamp={project.lastActivityAtMillis}/>},
    {label: 'Created', render: project => <TimestampToDate timestamp={project.createdAtMillis}/>},
    {label: 'Tags', render: project => project.tags.length > 0 ? project.tags.map(tag => `#${tag}`).join(', ') : null},
];

function CompareTable({projects, rows}: { projects: CompareProjectResponse[], rows: CompareRow[] }) {
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
                                <td key={`${project.ownerLogin}/${project.projectName}`}>
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

    // Restores the compare bar from the URL after a reload or when opened via a link
    useEffect(() => {
        if (projects && projects.length > 0) {
            replaceCompared?.(projects.map(it => ({ownerLogin: it.ownerLogin, name: it.projectName})));
        }
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
                    <CompareTable projects={projects} rows={ROWS.filter(row => visibleLabels.includes(row.label))}/>
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
