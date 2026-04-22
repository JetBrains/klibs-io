import React, {useMemo} from 'react';
import styles from "./styles.module.css"
import {PackageOverview, toTargetGroups} from "@/app/types";

import {Tag, presets} from '@rescui/tag';

interface TargetsListProps {
    projectPackage: PackageOverview;
}

const getSortedTargetGroups = (targets: PackageOverview['targets']) => {
    const targetGroups = toTargetGroups(targets);

    return targetGroups.map(platform => ({
        ...platform,
        groups: [...platform.groups].sort((a, b) => a.groupId.localeCompare(b.groupId))
    }));
};

export default function TargetsList({projectPackage}: TargetsListProps) {

    const sortedTargetGroups = useMemo(() => getSortedTargetGroups(projectPackage.targets), [projectPackage.targets]);

    return (
        <div className={styles.targetsList}>
            {sortedTargetGroups.map(platform => (
                <React.Fragment key={platform.platformId}>
                    {platform.groups.map(group => (
                        group.groupId && (
                            <Tag
                                key={group.groupId}
                                {...presets['filled-light']}
                            >
                                {group.groupId}
                            </Tag>
                        )
                    ))}
                </React.Fragment>
            ))}
        </div>
    )
}
