import {useMemo} from 'react';
import styles from "./styles.module.css"
import {getTargetGroupNames, PackageOverview} from "@/app/types";

import {Tag, presets} from '@rescui/tag';

interface TargetsListProps {
    projectPackage: PackageOverview;
}

export default function TargetsList({projectPackage}: TargetsListProps) {

    const targetGroups = useMemo(
        () => projectPackage.targetGroups ? getTargetGroupNames(projectPackage.targetGroups) : [],
        [projectPackage.targetGroups]
    );

    return (
        <div className={styles.targetsList}>
            {targetGroups.map(group => (
                <Tag
                    key={group}
                    {...presets['filled-light']}
                >
                    {group}
                </Tag>
            ))}
        </div>
    )
}
