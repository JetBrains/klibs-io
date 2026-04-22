// Project page
"use client"

import "github-markdown-css/github-markdown-light.css";
import styles from "./styles.module.css"

import {getPackageCoordinates, PackageOverview, ProjectDetails} from "@/app/types";
import {ProjectBreadcrumb} from "@/app/ui/breadcrumb";
import cn from "classnames";
import {ReactNode, useEffect, useState} from "react";

import PlatformsNames from "@/app/ui/platforms-names";
import {textCn} from "@rescui/typography";
import {cardCn} from "@rescui/card";
import Container from "@/app/ui/container";
import TargetsList from "@/app/ui/targets-list";

// Tab test
import {TabList, Tab, TabSeparator} from '@rescui/tab-list';
import SidePopup from "@/app/ui/side-popup";
import Tags from "@/app/ui/tags";
import {ProjectInfo} from "@/app/ui/project-info";

import Link from "next/link";
import {PackageImporterDropdown} from "@/app/ui/package-importer-dropdown";
import TargetsTablePopup from "@/app/ui/targets-table-popup";
import {trackEvent, GAEvent} from "@/app/analytics";

const CREATE_ISSUE_URL = 'https://github.com/JetBrains/klibs-io-issue-management/issues/new';

function buildSuggestEditUrl(projectOverview: ProjectDetails): string {
    const params = new URLSearchParams();
    params.append('url', `https://klibs.io/project/${projectOverview.ownerLogin}/${projectOverview.name}`);
    params.append('title', `[Edit project's metadata]: ${projectOverview.name}`);
    params.append('labels', 'enhancement');
    params.append('template', 'suggest_an_edit.yml');
    return `${CREATE_ISSUE_URL}?${params.toString()}`;
}

interface ProjectPageContentProps {
    initialProject: ProjectDetails;
    initialPackages: PackageOverview[];
    initialReadme: string;
    projectName: string;
}

export default function Project({
    initialProject,
    initialPackages,
    initialReadme,
    projectName,
}: ProjectPageContentProps) {
    const [projectOverview] = useState<ProjectDetails>(initialProject);
    const [projectPackages] = useState<PackageOverview[]>(initialPackages);
    const [projectReadme] = useState<string>(initialReadme);

    useEffect(() => {
        // Negative Y is for iOS safari
        window.scroll(0, -100);
    }, []);

    const [activeIndex, setActiveIndex] = useState(0);

    return (
        <Container className={"padding-bottom-large"}>

            {/*Title wrapper*/}
            <Container mode={"wrapper"} className={styles.titleWrapper}>
                {projectOverview &&
                    <>
                        {/*Breadcrumbs*/}
                        <ProjectBreadcrumb projectOverview={projectOverview}/>
                        {/* Project name */}
                        <h1 className={textCn('rs-h1')}>
                            {projectOverview.name}
                        </h1>
                    </>
                }
            </Container>

            {/*Body wrapper*/}
            <Container mode={"wrapper"} split>

                {/* Page body section*/}
                <Container mode={"wrapper"} className={styles.bodyWrapper}>

                    {/* Auto-gen data card */}
                    <div className={cn(cardCn({paddings: 16}), styles.card)}>
                        {/*Description*/}
                        {projectOverview && projectOverview.description &&
                            <p className={textCn('rs-text-2', {hardness: "hard"})}>
                                {projectOverview.description}
                            </p>
                        }
                        <div className={styles.descriptionRow}>
                            {projectOverview && projectOverview.tags && <Tags tags={projectOverview.tags} className={styles.tagsRow}/>}
                            <Link
                                className={cn(textCn('rs-text-3'), styles.editLink)}
                                href={buildSuggestEditUrl(projectOverview)}
                                onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Suggest an edit'})}}
                            >
                                Suggest an edit
                            </Link>
                        </div>
                    </div>

                    <Container mode="wrapper"
                               className={cn(styles.rightSideColumnWrapper, textCn('rs-text-2'), "hide-on-max hide-on-large")}>
                        <ProjectInfo projectOverview={projectOverview}/>
                    </Container>

                    {/*Project tabs desktop*/}
                    <ProjectTabs projectReadme={projectReadme} projectPackages={projectPackages}
                                 isMobile={false} activeIndex={activeIndex}
                                 setActiveIndex={setActiveIndex} projectName={projectName}></ProjectTabs>

                </Container>

                {/* Right-side page section */}
                <Container mode={"wrapper"} smallColumn>
                    <Container
                        mode="wrapper"
                        className={cn(
                            styles.rightSideColumnWrapper,
                            textCn('rs-text-2'),
                            "hide-on-medium hide-on-small"
                        )}
                    >

                        <ProjectInfo projectOverview={projectOverview}/>
                    </Container>
                </Container>
            </Container>

            {/*Project tabs mobile and tablet*/}
            <ProjectTabs projectReadme={projectReadme} projectPackages={projectPackages} isMobile={true}
                         activeIndex={activeIndex} setActiveIndex={setActiveIndex} projectName={projectName}></ProjectTabs>
        </Container>
    );
}

interface ProjectTabsProps {
    isMobile: boolean;
    projectReadme: string | null;
    projectPackages: PackageOverview[] | null
    activeIndex: number;
    setActiveIndex: (index: number) => void;
    projectName: string;
}


function ProjectTabs({projectReadme, projectPackages, isMobile, activeIndex, setActiveIndex, projectName}: ProjectTabsProps) {

    // Tabs
    const ContentSwitcher = ({index, children}: { index: number; children: Array<ReactNode> }) => {
        return children[index];
    };

    return (
        <div
            className={cn(styles.projectTabsWrapper, isMobile ? styles.projectTabsWrapperMobile : styles.projectTabsWrapperDesktop)}>
            {/* Tabs */}
            <Container mode={"wrapper"}>
                {/*Tab headers*/}
                <TabList
                    value={activeIndex}
                    onChange={v => setActiveIndex(v)}
                    mode={"rock"}
                >
                    <Tab className={"rs-tab-override"} onClick={() => trackEvent(GAEvent.PROJECT_README_TAB_CLICK, {eventCategory: projectName})}>Readme</Tab>
                    <Tab className={"rs-tab-override"} onClick={() => trackEvent(GAEvent.PROJECT_PACKAGES_TAB_CLICK, {eventCategory: projectName})}>Packages</Tab>
                </TabList>

                <TabSeparator/>

                {/*Tab content*/}
                <ContentSwitcher index={activeIndex}>

                    {/*Readme*/}
                    <Container mode="wrapper" className="markdown-body">
                        {projectReadme &&
                            <div data-testid="readme-tab" dangerouslySetInnerHTML={{__html: projectReadme}}></div>
                        }
                    </Container>

                    {/*Package list*/}
                    <div>
                        {/*Table heading*/}
                        <div
                            className={cn(textCn('rs-text-3', {hardness: "hard"}), styles.packageListTableHeading)}>
                            <div>
                                Package id
                            </div>
                            <div>
                                Supported platforms and targets
                            </div>
                        </div>
                        {/*Table body*/}
                        <div>
                            {/*Table card */}
                            <>
                                {projectPackages && projectPackages.map(packageOverview =>
                                    <Link key={packageOverview.id}
                                          href={`/package/${packageOverview.groupId}/${packageOverview.artifactId}`}
                                          className={styles.packageListLink}
                                          onClick={() => trackEvent(GAEvent.PROJECT_PACKAGE_CLICK, {eventCategory: projectName, eventLabel: getPackageCoordinates(packageOverview)})}
                                    >
                                        <div
                                            className={cn(textCn('rs-text-3'), "align-middle", "table-primary", styles.packageListTableItem)}
                                        >
                                            {/*Package coordinates*/}
                                            <div className={styles.packageCoordinates}>
                                                <Link
                                                    href={`/package/${packageOverview.groupId}/${packageOverview.artifactId}`}
                                                    className={styles.packageCoordinatesTitle}>
                                                    {getPackageCoordinates(packageOverview)}
                                                </Link>
                                                <div className={textCn('rs-text-3', {hardness: 'hard'})}>
                                                    {packageOverview.description}
                                                </div>
                                            </div>

                                            {/* List of targets */}
                                            {packageOverview.targets && packageOverview.targets.length &&
                                                <>
                                                    <div className={styles.platformsAndTargetsWrapper}>
                                                        <div className={styles.listWrapper}>
                                                            <PlatformsNames packageOverview={packageOverview}/>
                                                            {packageOverview.targets && packageOverview.targets.length > 0 &&
                                                                <SidePopup
                                                                    target={
                                                                        <TargetsList
                                                                            projectPackage={packageOverview}/>
                                                                    }
                                                                >
                                                                    <TargetsTablePopup
                                                                        projectPackage={packageOverview}/>
                                                                </SidePopup>
                                                            }
                                                        </div>
                                                        {/*Dropdown, not visible on mobile*/}
                                                        <div className={styles.packageDropdown}>
                                                            <PackageImporterDropdown
                                                                packageOverview={packageOverview}/>
                                                        </div>
                                                    </div>
                                                </>
                                            }

                                        </div>
                                    </Link>
                                )}
                            </>
                        </div>
                    </div>

                </ContentSwitcher>

            </Container>
        </div>
    )
}
