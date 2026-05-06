import {Fragment, ReactNode, useMemo} from "react";
import Link from "next/link";
import cn from 'classnames';
import {PackageSearchResults} from "@/app/types";
import {cardCn} from '@rescui/card';
import {textCn} from '@rescui/typography';
import {ReadIcon} from '@rescui/icons';
import {Tag, presets} from '@rescui/tag';
import {getPlatformName, mapNativeTargetToGroupName} from "@/app/types";
import {Platform} from "@/app/types";
import PlatformTag from "@/app/ui/platform-tag";

import styles from './styles.module.css';
import {trackEvent, GAEvent} from "@/app/analytics";

interface PackageCardProps {
    featuredPackage?: PackageSearchResults;
    className?: string;
    search?: string;
}

function SearchTextWrap({search, children}: { search?: PackageCardProps['search'], children?: string | null }) {
    return useMemo(() => {
        if (!search || !children) return children;

        const chunks: ReactNode[] = [];
        let text = children;
        const query = search.toLowerCase();

        do {
            const i = text.toLowerCase().indexOf(query);

            if (i === -1) {
                text && chunks.push(<Fragment key={i}>{text}</Fragment>);
                break
            }

            const item = text.substring(0, i);
            const suffixIndex = i + query.length;

            chunks.push(<Fragment key={i + '-' + text.length}>{item}<span
                className={styles.highlight}>{text.substring(i, suffixIndex)}</span></Fragment>);
            text = text.substring(suffixIndex);
        } while (true);

        if (chunks.length === 1) return chunks[0];
        return <>{chunks}</>
    }, [search, children]);
}

function getVersionedPlatformsFromTargets(targets: string[]) {
    return targets.filter(target => target.startsWith("JVM:") || target.startsWith("ANDROID_JVM:")).map(target => {
        const [platform, version] = target.split(":");
        if (platform === "ANDROID_JVM") {
            return `AndroidJVM ${version}`;
        }
        return `${platform} ${version}`;
    });
}

function getNativePlatformGroups(targets: string[]): string[] {
    const nativeGroups = new Set<string>();
    for (const target of targets) {
        const [platform, groupTarget] = target.split(":");
        if (platform === "NATIVE") {
            const group = groupTarget.split("_")[0]; // Take only the first part of group_target
            nativeGroups.add(group);
        }
    }
    return Array.from(nativeGroups);
}

// Helper function to generate package link
function getPackageLink(packageData: PackageSearchResults) {
    return `/package/${packageData.groupId}/${packageData.artifactId}`;
}

export default function PackageCard({featuredPackage, className, search}: PackageCardProps) {
    const packageLink = featuredPackage ? getPackageLink(featuredPackage) : null;

    const versionedPlatforms = featuredPackage?.targets && getVersionedPlatformsFromTargets(featuredPackage?.targets);
    const nonVersionedPlatforms = featuredPackage?.platforms.filter(platform => platform !== Platform.common && platform !== Platform.native && platform !== Platform.jvm && platform !== Platform.androidJvm)
    const nativeTargets = featuredPackage?.targets && getNativePlatformGroups(featuredPackage?.targets);

    return (
        <Link
            className={cn(cardCn({
                isClickable: true,
                paddings: 16,
            }), styles.card, className)}
            href={String(packageLink)}
            title={featuredPackage && `${featuredPackage.groupId}:${featuredPackage.artifactId}`}
            onClick={() => trackEvent(GAEvent.PACKAGE_CARD_CLICK, {eventCategory: `${featuredPackage?.groupId}:${featuredPackage?.artifactId}`})}
        >
            {featuredPackage &&
                <>
                    {/*Heading row*/}
                        <div className={styles.headingWrapper}>
                            {/*Title & author*/}
                            <div className={styles.headingLeftSideWrapper}>
                                {/*Title*/}
                                <h4 className={cn(textCn('rs-h4'), styles.cardHeading)}>
                                    <SearchTextWrap
                                        search={search}>{`${featuredPackage.groupId}:${featuredPackage.artifactId}`}</SearchTextWrap>
                                </h4>

                                {/*Author id*/}
                                <p className={cn(textCn('rs-text-3', {hardness: 'hard'}))}>
                                    by {featuredPackage.ownerLogin}
                                </p>
                            </div>
                        </div>

                    {/*Description*/}
                        <p className={cn(textCn('rs-text-3', {hardness: 'hard'}), styles.cardDescription)}>
                            <SearchTextWrap search={search}>{featuredPackage.description}</SearchTextWrap>
                        </p>

                    {/*Footer section*/}
                        <div className={styles.footerWrapper}>
                            {/*License*/}
                            <div className={styles.footerRow}>
                                <ReadIcon size={'m'} className={'card__icon'}/>
                                <p className={cn(textCn('rs-text-3'))}>
                                    {featuredPackage.licenseName || 'Unknown license'}
                                </p>
                            </div>

                            {/*Platforms and targets*/}
                            <div className={styles.platforms}>
                                {versionedPlatforms?.length ? versionedPlatforms.map(platform => (
                                    <PlatformTag key={platform}>
                                        {platform}
                                    </PlatformTag>
                                )) : null}
                                {nonVersionedPlatforms?.length ? nonVersionedPlatforms.map(platform => (
                                    <PlatformTag key={platform}>
                                        {getPlatformName(platform)}
                                    </PlatformTag>
                                )) : null}
                                {nativeTargets?.length ? nativeTargets.map(target => (
                                    <Tag
                                        key={target}
                                        {...presets['filled-light']}
                                    >
                                        {mapNativeTargetToGroupName(target)}
                                    </Tag>
                                )) : null}
                            </div>
                        </div>
                </>
            }
        </Link>
    );
}
