'use client'

import {ReactNode} from "react";
import Link from "next/link";
import cn from "classnames";
import {tableCn, tableOverflowWrapperCn} from "@rescui/table";
import {Tag} from "@rescui/tag";
import {textCn} from "@rescui/typography";

import {CompareProjectResponse, getPlatformName, kFormatter, Platform, platformOrder} from "@/app/types";
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

const ROWS: CompareRow[] = [
    {label: 'Description', render: project => project.description},
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

function CompareTable({projects}: { projects: CompareProjectResponse[] }) {
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
                    {ROWS.map(row => (
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

export default function ComparePageContent({projects}: ComparePageContentProps) {
    if (projects === null) return <ContentFailed/>;

    return (
        <Container mode="container" className="padding-top-medium padding-bottom-large">
            <Container mode="wrapper">
                <h1 className={cn(textCn('rs-h1'), styles.title)}>Compare projects</h1>
                {projects.length > 0 ? <CompareTable projects={projects}/> :
                    <p className={textCn('rs-text-2', {hardness: 'hard'})}>
                        Nothing to compare yet. Use the Compare toggle on project cards
                        to <Link href="/">pick projects</Link>.
                    </p>
                }
            </Container>
        </Container>
    );
}
