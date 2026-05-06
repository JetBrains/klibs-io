// Package page
"use client"

import "github-markdown-css/github-markdown-light.css";
import styles from "./styles.module.css"

import {useEffect, useState} from "react";
import Link from "next/link";
import cn from "classnames";
import {
    getProjectLink,
    kFormatter,
    PackageDetails,
    PackageOverview,
    ProjectDetails,
} from "@/app/types";
import TimeAgo from "@/app/ui/time-ago";
import {TimestampToDate} from "@/app/ui/time-ago";
import PlatformsNames from "@/app/ui/platforms-names";
import {textCn} from "@rescui/typography";

import {TabList, Tab} from '@rescui/tab-list';

import {RocketIcon, FilesIcon, KotlinIcon, CompanyIcon, TeamIcon, GearIcon, GlobusIcon} from '@rescui/icons';
import {tableCn} from "@rescui/table";
import TargetsTable from "@/app/ui/targets-table";
import Container from "@/app/ui/container";
import {PackageBreadcrumbs} from "@/app/ui/breadcrumb";
import {PackageImportCode} from "@/app/ui/package-importer";
import {PackageImporterDropdown} from "@/app/ui/package-importer-dropdown";

import TargetsList from "@/app/ui/targets-list";

import SidePopup from "@/app/ui/side-popup";
import TargetsTablePopup from "@/app/ui/targets-table-popup";
import {trackEvent, GAEvent} from "@/app/analytics";

interface PackagePageContentProps {
    initialPackage: PackageDetails;
    initialParentProject: ProjectDetails | null;
    initialPackageVersions: PackageOverview[];
    initialGroupArtifacts: PackageOverview[];
    version?: string;
}

export default function Package({
    initialPackage,
    initialParentProject,
    initialPackageVersions,
    initialGroupArtifacts,
    version,
}: PackagePageContentProps) {
    const [projectPackage] = useState<PackageDetails>(initialPackage);
    const [parentProject] = useState<ProjectDetails | null>(initialParentProject);
    const [packageVersions] = useState<PackageOverview[]>(initialPackageVersions);
    const [projectPackages] = useState<PackageOverview[]>(initialGroupArtifacts);

    useEffect(() => {
        // Negative Y is for iOS safari
        window.scroll(0, -100);
    }, []);

    return (
        <Container mode={"container"} className={cn(textCn('rs-text-2', {hardness: "hard"}), "padding-bottom-large")}>

            {/*Title wrapper*/}
            <Container mode={"wrapper"} className={styles.titleWrapper}>
                {projectPackage &&
                    <>
                        {/*Breadcrumbs*/}
                        {<PackageBreadcrumbs
                            version={version}
                            projectPackage={projectPackage}
                            projectPackages={projectPackages}
                            packageVersions={packageVersions}
                            parentProject={parentProject}
                        />}
                        {/* Project name */}
                        <h2 className={textCn('rs-h2')}>
                            {projectPackage.artifactId}:{projectPackage.version}
                        </h2>
                    </>
                }
            </Container>

            {/*Body section*/}
            <Container mode={"wrapper"} split>

                {/*Page body */}
                <Container mode={"wrapper"} className={styles.bodyWrapper}>

                    {/*Metadata section*/}
                    <div className={styles.metadataWrapper}>
                        {/*Latest release*/}
                        <div className={styles.metadataNode}>
                            <RocketIcon className={styles.metadataIcon}/>
                            <span>Latest release</span>
                            <span>
                                {packageVersions && packageVersions.length &&
                                    <>{packageVersions[0].version} (<TimeAgo
                                        timestamp={packageVersions[0].releasedAtMillis}/>)</>
                                }
                            </span>
                        </div>

                        {/*Parent project*/}
                        <div className={styles.metadataNode}>
                            <CompanyIcon className={styles.metadataIcon}/>
                            <span>Parent project</span>
                            {parentProject &&
                                <Link
                                    href={getProjectLink(parentProject)}
                                    onClick={() => {
                                        trackEvent(GAEvent.PACKAGE_PAGE_LINK_CLICK, {
                                            eventCategory: `${projectPackage.artifactId}:${projectPackage.version}`,
                                            eventLabel: 'Parent project'
                                        })
                                    }}
                                >
                                    {parentProject.name} ({kFormatter(parentProject.scmStars)} stars)
                                </Link>
                            }
                        </div>

                        {/*Kotlin version*/}
                        <div className={styles.metadataNode}>
                            <KotlinIcon className={styles.metadataIcon}/>
                            <span>Kotlin version</span>
                            <span>{projectPackage.kotlinVersion}</span>
                        </div>

                        {/*License*/}
                        <div className={styles.metadataNode}>
                            <FilesIcon className={styles.metadataIcon}/>
                            <span>License</span>
                            {projectPackage.licenses && projectPackage.licenses.map(license =>
                                license.url ?
                                    <Link
                                        href={license.url}
                                        target="_blank"
                                        key={license.title}
                                        onClick={() => {
                                            trackEvent(GAEvent.PACKAGE_PAGE_LINK_CLICK, {
                                                eventCategory: `${projectPackage.artifactId}:${projectPackage.version}`,
                                                eventLabel: 'License'
                                            })
                                        }}
                                    >
                                        {license.title}
                                    </Link>
                                    : <span key={license.title}>{license.title}</span>
                            )}
                        </div>

                        {/*Developer*/}
                        {/*Link seems incorrect, androidx.coreLcore-bundle leads to jb website*/}
                        <div className={styles.metadataNode}>
                            <TeamIcon className={styles.metadataIcon}/>
                            <span>Developer</span>
                            {projectPackage.developers.map(developer =>
                                developer.url ?
                                    <Link
                                        href={developer.url}
                                        target="_blank"
                                        key={developer.title}
                                        onClick={() => {
                                            trackEvent(GAEvent.PACKAGE_PAGE_LINK_CLICK, {
                                                eventCategory: `${projectPackage.name}`,
                                                eventLabel: 'Developer'
                                            })
                                        }}
                                    >
                                        {developer.title}
                                    </Link> :
                                    <span key={developer.title}>{developer.title}</span>
                            )}
                        </div>

                        {/*Gradle version*/}
                        {/*See if build tool version always returns gradle version*/}
                        <div className={styles.metadataNode}>
                            <GearIcon className={styles.metadataIcon}/>
                            <span>Build tool version</span>
                            <span>{projectPackage.buildTool}</span>
                        </div>
                    </div>

                    {/*Right-side column content for smaller screens*/}
                    <Container mode={"wrapper"} className={cn(styles.bodyWrapper, "hide-on-max hide-on-large")}
                               smallColumn>
                        <TargetsTable projectPackage={projectPackage}/>
                    </Container>

                    {/*Implementation code snippet*/}
                    <PackageImportCode projectPackage={projectPackage}/>

                    {/*Name, description, links, tags section*/}
                    <div className={styles.infoWrapper} data-testid="package-description">
                        {projectPackage &&
                            <h3 className={textCn("rs-h3")}>
                                {projectPackage.name}
                            </h3>
                        }

                        {projectPackage.description &&
                            <p>{projectPackage.description}</p>
                        }

                        {/* Links */}
                        <div className={styles.linkWrapper}>
                            <GlobusIcon className={"hide-on-small"}/>
                            {projectPackage.linkHomepage &&
                                <Link
                                    href={projectPackage.linkHomepage}
                                    onClick={() => {
                                        trackEvent(GAEvent.PACKAGE_PAGE_LINK_CLICK, {
                                            eventCategory: `${projectPackage.name}`,
                                            eventLabel: 'Metadata homepage'
                                        })
                                    }}
                                >
                                    Homepage
                                </Link>
                            }

                            {/*Line*/}
                            {projectPackage.linkHomepage && projectPackage.linkScm &&
                                <div className={cn(styles.linkDivider, "hide-on-small")}></div>
                            }

                            {projectPackage.linkScm &&
                                <Link
                                    href={projectPackage.linkScm}
                                    onClick={() => {
                                        trackEvent(GAEvent.PACKAGE_PAGE_LINK_CLICK, {
                                            eventCategory: `${projectPackage.name}`,
                                            eventLabel: 'Source code management'
                                        })
                                    }}
                                >
                                    Source code management
                                </Link>
                            }

                            {/*Line*/}
                            {projectPackage.linkScm && projectPackage.linkFiles &&
                                <div className={cn(styles.linkDivider, "hide-on-small")}></div>
                            }

                            {projectPackage.linkFiles &&
                                <Link
                                    href={projectPackage.linkFiles}
                                    onClick={() => {
                                        trackEvent(GAEvent.PACKAGE_PAGE_LINK_CLICK, {
                                            eventCategory: `${projectPackage.name}`,
                                            eventLabel: 'Files'
                                        })
                                    }}
                                >
                                    Maven artifacts
                                </Link>
                            }
                        </div>
                    </div>

                    {/*Version history table desktop*/}
                    <VersionHistoryTable
                        isMobile={false}
                        packageVersions={packageVersions}
                        version={version}
                        packageName={projectPackage.name}
                    />

                </Container>

                {/*Right-side column*/}
                <Container mode={"wrapper"} className={cn(styles.bodyWrapper, "hide-on-medium hide-on-small")}
                           smallColumn>
                    <TargetsTable projectPackage={projectPackage}/>
                </Container>

            </Container>

            {/*Version history table mobile*/}
            <VersionHistoryTable
                isMobile={true}
                packageVersions={packageVersions}
                version={version}
                packageName={projectPackage.name}
            />

        </Container>
    );
}

interface VersionHistoryTableProps {
    isMobile: boolean;
    packageVersions: PackageOverview[] | null;
    version?: string;
    packageName: string | null;
}

function VersionHistoryTable({isMobile, packageVersions, version, packageName}: VersionHistoryTableProps) {

    // Tabs, placeholder for additional details tab
    const [activeIndex, setActiveIndex] = useState(0);

    return (
        <div
            className={cn(styles.versionHistoryWrapper, isMobile ? styles.versionHistoryWrapperMobile : styles.versionHistoryWrapperDesktop)}>
            <div className={styles.versionHistoryTabs}>
                <TabList
                    value={activeIndex}
                    onChange={v => setActiveIndex(v)}
                    mode={"rock"}
                >
                    <Tab className={"rs-tab-override"}>Version history</Tab>
                    {/*<Tab className={"rs-tab-override"}>Additional details</Tab>*/}
                </TabList>
            </div>

            <div>
                <table className={cn(tableCn({isWide: true, size: 'm'}), styles.versionTable)}>
                    <thead className={styles.tableHead}>
                    <tr className={textCn("rs-text-3", {hardness: "hard"})}>
                        <th className="padding-left-medium">Version</th>
                        <th>Release</th>
                        <th>Platforms and targets</th>
                    </tr>
                    </thead>

                    <tbody className={styles.tableBody}>
                    {packageVersions?.map(packageVersion =>
                        <Link
                            href={`/package/${packageVersion.groupId}/${packageVersion.artifactId}/${packageVersion.version}`}
                            className={styles.tableRowLink}
                            key={`${packageVersion.id}-${packageVersion.version}`}
                            onClick={() => {trackEvent(GAEvent.PACKAGE_VERSION_LINK_CLICK, {eventCategory: packageName, eventLabel: packageVersion.version})}}
                        >
                            <tr className={cn("align-middle", styles.tableRow, textCn("rs-text-3", {hardness: "hard"}))}>
                                {/*Package version*/}
                                <td className={cn("padding-left-medium", styles.tableCell, styles.versionCell)}>
                                    {version != packageVersion.version ? (
                                        <Link
                                            href={`/package/${packageVersion.groupId}/${packageVersion.artifactId}/${packageVersion.version}`}
                                            className={styles.packageVersionLink}
                                        >
                                            {packageVersion.version}
                                        </Link>
                                    ) : (
                                        <span>{packageVersion.version}</span>
                                    )}
                                </td>

                                {/* Release date */}
                                <td>
                                    <div className={styles.packageReleaseDate}>
                                        <div
                                            className={styles.packageReleaseDateMobileTitle}>Release:&nbsp;</div>
                                        <TimestampToDate timestamp={packageVersion.releasedAtMillis}/>
                                    </div>
                                </td>

                                {/* Platforms and targets */}
                                <td className={styles.platformsAndTargetsCell}>
                                    <div className={styles.platformsTargetsWrapper}>
                                        <div className={styles.platformsListWrapper}>
                                            <PlatformsNames packageOverview={packageVersion}/>
                                            <SidePopup
                                                target={
                                                    <TargetsList projectPackage={packageVersion}/>
                                                }
                                            >
                                                <TargetsTablePopup projectPackage={packageVersion}/>
                                            </SidePopup>
                                        </div>
                                        <div className={styles.packageDropdown}>
                                            <PackageImporterDropdown packageOverview={packageVersion}
                                                                     isPackagePage/>
                                        </div>
                                    </div>

                                </td>


                            </tr>
                        </Link>
                    )}
                    </tbody>
                </table>
            </div>

            <span className={textCn("rs-text-3")}>
                *Only more recent versions are displayed
            </span>
        </div>
    );
}
