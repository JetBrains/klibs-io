import styles from "./styles.module.css"
import cn from "classnames";
import {textCn} from "@rescui/typography";
import {PackageOverview, toTargetGroups} from "@/app/types";

interface TargetsTableProps {
    projectPackage: PackageOverview;
}

export default function TargetsTable({projectPackage} : TargetsTableProps) {
    return (
        // Wrapper
        <div className={cn(styles.wrapper, textCn("rs-text-3", {hardness: "hard"}))} >

            {toTargetGroups(projectPackage.targets).map(platform => (

                // Platform wrapper
                <div key={platform.platformId} className={cn(styles.platformWrapper, styles[platform.platformName.substring(0,2)])}>

                    {/*Platform label*/}
                    <div className={styles.platformId}>
                        {platform.platformName}
                    </div>

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
