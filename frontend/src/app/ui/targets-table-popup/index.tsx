import styles from "../targets-table/styles.module.css"
import cn from "classnames";
import {textCn} from "@rescui/typography";
import {PackageOverview, toTargetGroups} from "@/app/types";

interface TargetsTableProps {
    projectPackage: PackageOverview;
}

export default function TargetsTablePopup({projectPackage} : TargetsTableProps) {

    return (
        // Wrapper
        <div className={cn(textCn("rs-text-3", {hardness: "hard"}))} >

            {toTargetGroups(projectPackage.targets, true).map(platform => (

                // Platform wrapper
                <div key={platform.platformId} className={cn(styles.platformWrapper, styles[platform.platformName.substring(0,2)])}>

                    {/*Group+target wrapper*/}
                    <div className={styles.groupTargetWrapper}>
                        {platform.groups.sort((a, b) => a.groupId.localeCompare(b.groupId)).map(group => (

                            // Group wrapper
                            <div key={group.groupId} className={styles.groupWrapper}>

                                {/*Group label*/}
                                <div className={cn(textCn("rs-h5"), styles.groupId)}>
                                    {group.groupId}
                                </div>

                                 {/*Target wrapper*/}
                                 <div className={styles.targetWrapper}>

                                    {/* List of all targets*/}
                                    {group.targets.map(target => (
                                        <span key={target}>
                                            {target}
                                        </span>
                                    ))}

                                 </div>
                            </div>
                        ))}
                    </div>
                </div>
            ))}
        </div>

    )
}
