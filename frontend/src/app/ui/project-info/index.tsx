import {getOwnerLink, hasAnyLink, isFeaturedProject, isGrantWinner, ProjectDetails, sortedPlatforms} from "@/app/types";
import PlatformBadge from "@/app/ui/platform-badge";
import Link from "next/link";
import TimeAgo from "@/app/ui/time-ago";
import cn from "classnames";
import styles from "./styles.module.css";
import FeaturedLabel from "@/app/ui/featured-label";
import {trackEvent, GAEvent} from "@/app/analytics";

export function ProjectInfo({projectOverview}: {projectOverview: ProjectDetails}) {
    // Owner link for metadata while it is not in a separate component
    const ownerLink = getOwnerLink(projectOverview);
    const showGrantWinner = isGrantWinner(projectOverview);
    const showFeaturedProject = isFeaturedProject(projectOverview);

    return (
        <>
            {/* Platforms */}
            <div className={styles.platformTagWrapper}>
                {projectOverview && sortedPlatforms(projectOverview.platforms).map(platform =>
                    <PlatformBadge key={platform} platform={platform}/>
                )}
            </div>

            {/* Metadata section 1 */}
            <div className={styles.metadataWrapper}>

                {/* GitHub stars */}
                <div>
                    <span>GitHub stars</span>
                    <span className={styles.dataValue}>{projectOverview && projectOverview.scmStars}</span>
                </div>

                {/*Authors*/}
                <div>
                    <span>Authors</span>
                    <Link href={ownerLink} onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Authors'})}}>
                        {projectOverview && projectOverview.ownerLogin}
                    </Link>
                </div>

                {/*Open issues*/}
                <div>
                    <span>Open issues</span>
                    {projectOverview && projectOverview.openIssues && projectOverview.linkIssues &&
                        <Link href={projectOverview.linkIssues} target="_blank" onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Open issues'})}}>
                            {projectOverview.openIssues}
                        </Link>
                    }
                </div>

                {/*License*/}
                {projectOverview && projectOverview.licenseName &&
                    <div>
                        <span>License</span>
                        <span className={styles.dataValue}>{projectOverview.licenseName}</span>
                    </div>
                }

                <div>
                    <span>Creation date</span>

                    <span className={styles.dataValue}>
						{projectOverview && <TimeAgo timestamp={projectOverview.createdAtMillis}/>}
					</span>
                </div>
            </div>

            <hr></hr>

            {/*Metadata section 2*/}
            <div className={styles.metadataWrapper}>
                <div>
                    <span>Last activity</span>
                    <span className={styles.dataValue}>
						{projectOverview && <TimeAgo timestamp={projectOverview.lastActivityAtMillis}/>}
					</span>
                </div>
                {/*Latest version and latest release date*/}
                <div>
                    <span>Latest release</span>
                    <span className={styles.dataValue}>
						{
                            projectOverview && projectOverview.latestReleasePublishedAtMillis && <>
                                {projectOverview.latestReleaseVersion} (<TimeAgo timestamp={projectOverview.latestReleasePublishedAtMillis}/>)
                            </>
                        }
					</span>
                </div>
            </div>

            <hr></hr>

            {/*Metadata section 3*/}
            <div className={cn(styles.metadataWrapper, styles.linkWrapper)}>
                {/* Links */}
                {projectOverview && hasAnyLink(projectOverview) &&
                    <>
                      {projectOverview.linkHomepage && <Link href={projectOverview.linkHomepage} target="_blank" onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Homepage'})}}>Homepage</Link>}
                      {projectOverview.linkScm && <Link href={projectOverview.linkScm} target="_blank" onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Github repository'})}}>GitHub repository</Link>}
                      {projectOverview.linkGitHubPages && <Link href={projectOverview.linkGitHubPages} target="_blank" onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Github pages'})}}>GitHub pages</Link>}
                      {projectOverview.linkWiki && <Link href={projectOverview.linkWiki} target="_blank" onClick={() => {trackEvent(GAEvent.PROJECT_INFO_LINK_CLICK, {eventCategory: projectOverview.name, eventLabel: 'Wiki Page'})}}>Wiki page</Link>}
                    </>
                }
            </div>

            {(showGrantWinner || showFeaturedProject) &&
                <div className={styles.featuredLabelBlock}>
                    {showGrantWinner && <FeaturedLabel isGrantWinner/>}

                    {showFeaturedProject && <FeaturedLabel isFeaturedProject/>}
                </div>
            }
        </>
    );
}
