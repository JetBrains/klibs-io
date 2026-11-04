import Link from "next/link";
import cn from 'classnames';
import { textCn } from '@rescui/typography';
import { getPackageTargetGroupLabels, PackageSearchResults } from "@/app/types";
import PlatformTag from "@/app/ui/platform-tag";
import SearchTextWrap from "@/app/ui/search-text-wrap";
import CardDetails, { CardDetail } from "@/app/ui/card-details";
import TimeAgo from "@/app/ui/time-ago";

import styles from './styles.module.css';
import { trackEvent, GAEvent } from "@/app/analytics";

interface PackageCardHorizontalProps {
    pkg: PackageSearchResults;
    className?: string;
    search?: string;
}

export default function PackageCardHorizontal({ pkg, className, search }: PackageCardHorizontalProps) {
    const coordinates = `${pkg.groupId}:${pkg.artifactId}`;
    const targetGroups = pkg.targetGroups ? getPackageTargetGroupLabels(pkg.targetGroups) : [];

    const details: CardDetail[] = [
        { label: 'License', value: pkg.licenseName || 'Unknown license' },
        {
            label: 'Latest release',
            value: <>{pkg.latestVersion} (<TimeAgo timestamp={pkg.releaseTsMillis}/>)</>,
        },
    ];

    return (
        <Link
            className={cn(styles.card, className)}
            href={`/package/${pkg.groupId}/${pkg.artifactId}`}
            title={coordinates}
            onClick={() => trackEvent(GAEvent.PACKAGE_CARD_CLICK, { eventCategory: coordinates })}
        >
            <div className={styles.content}>
                {/*Title & author*/}
                <div className={styles.heading}>
                    <h3 className={cn(textCn('rs-h3'), styles.title)}>
                        <SearchTextWrap search={search}>{coordinates}</SearchTextWrap>
                    </h3>
                    <p className={cn(textCn('rs-text-3'), styles.author)}>
                        by {pkg.ownerLogin}
                    </p>
                </div>

                {/*Description*/}
                {pkg.description &&
                    <p className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.description)}>
                        <SearchTextWrap search={search}>{pkg.description}</SearchTextWrap>
                    </p>
                }

                {/*Target groups*/}
                {targetGroups.length > 0 &&
                    <div className={styles.platforms}>
                        {targetGroups.map(group => (
                            <PlatformTag key={group}>{group}</PlatformTag>
                        ))}
                    </div>
                }
            </div>

            <CardDetails rows={details} className={styles.details}/>
        </Link>
    );
}
