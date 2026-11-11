import Link from "next/link";
import cn from 'classnames';
import { textCn } from '@rescui/typography';
import { WinIcon } from '@rescui/icons';
import { getProjectLink, getTargetGroupNames, isGrantWinner, ProjectSearchResults } from "@/app/types";
import PlatformTag from "@/app/ui/platform-tag";
import CompareToggle from "@/app/ui/compare-toggle";
import SearchTextWrap from "@/app/ui/search-text-wrap";
import CardDetails, { CardDetail } from "@/app/ui/card-details";
import TimeAgo from "@/app/ui/time-ago";

import styles from './styles.module.css';
import { trackEvent, GAEvent } from "@/app/analytics";
import { ANDROIDX_OWNER_LOGIN, JETBRAINS_KOTLIN_OWNER_LOGINS } from "@/app/constants";

interface ProjectCardHorizontalProps {
    project: ProjectSearchResults;
    className?: string;
    search?: string;
}

export default function ProjectCardHorizontal({ project, className, search }: ProjectCardHorizontalProps) {
    const targetGroups = project.targetGroups ? getTargetGroupNames(project.targetGroups) : [];
    const hasTags = project.tags && project.tags.length > 0;
    const showGrantWinner = isGrantWinner(project);
    const isJetBrainsKotlinOwner = JETBRAINS_KOTLIN_OWNER_LOGINS.some(ownerLogin => ownerLogin === project.ownerLogin);
    const affiliation = isJetBrainsKotlinOwner
        ? { type: 'jetbrains-kotlin' as const }
        : project.ownerLogin === ANDROIDX_OWNER_LOGIN
            ? { type: 'androidx' as const }
            : null;

    const details: CardDetail[] = [
        { label: 'GitHub stars', value: project.scmStars },
        { label: 'Dependents', value: project.dependentCount },
        { label: 'License', value: project.licenseName || 'Unknown license' },
    ];

    if (project.latestReleaseVersion) {
        details.push({
            label: 'Latest release',
            value: project.latestReleasePublishedAtMillis
                ? <>{project.latestReleaseVersion} (<TimeAgo timestamp={project.latestReleasePublishedAtMillis}/>)</>
                : project.latestReleaseVersion,
        });
    }

    return (
        <Link
            className={cn(styles.card, className, {
                [styles.grantProject]: showGrantWinner,
                [styles.jetbrainsProject]: affiliation?.type === 'jetbrains-kotlin' && !showGrantWinner,
                [styles.androidxProject]: affiliation?.type === 'androidx' && !showGrantWinner,
            })}
            data-project-affiliation={affiliation?.type}
            href={getProjectLink(project)}
            title={project.name}
            onClick={() => trackEvent(GAEvent.PROJECT_CARD_CLICK, { eventCategory: project.name })}
        >
            <div className={styles.content}>
                {/*Title & author*/}
                <div className={styles.heading}>
                    <h3 className={cn(textCn('rs-h3'), styles.title)}>
                        <SearchTextWrap search={search}>{project.name}</SearchTextWrap>
                    </h3>
                    <div className={styles.authorRow}>
                        <p className={cn(textCn('rs-text-3'), styles.author)}>
                            by {project.ownerLogin}
                        </p>

                        {showGrantWinner &&
                            <div className={styles.grantWinner}>
                                <WinIcon size={'m'} className={styles.grantWinnerIcon}/>
                                <span className={cn(textCn('rs-h5'), styles.grantWinnerTitle)}>Kotlin grant winner</span>
                            </div>
                        }
                    </div>
                </div>

                {/*Description*/}
                {project.description &&
                    <p className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.description)}>
                        <SearchTextWrap search={search}>{project.description}</SearchTextWrap>
                    </p>
                }

                {/*Tags*/}
                {hasTags &&
                    <div className={styles.tags}>
                        {project.tags.map(tag => (
                            <span key={tag} className={textCn('rs-text-3')}>
                                <SearchTextWrap search={search}>{`#${tag}`}</SearchTextWrap>
                            </span>
                        ))}
                    </div>
                }

                {/*Target groups*/}
                {targetGroups.length > 0 &&
                    <div className={styles.platforms}>
                        {targetGroups.map(group => (
                            <PlatformTag key={group}>{group}</PlatformTag>
                        ))}
                    </div>
                }

                <CompareToggle ownerLogin={project.ownerLogin} name={project.name} className={styles.compare}/>
            </div>

            <CardDetails rows={details} className={styles.details}/>
        </Link>
    );
}
