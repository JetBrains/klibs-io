import {getPlatformName, getUniquePlatforms, PackageOverview, Platform} from "@/app/types";
import PlatformTag from "@/app/ui/platform-tag";
import styles from './styles.module.css';

export default function PlatformsNames({packageOverview}: {packageOverview: PackageOverview}) {
    const platforms = getUniquePlatforms(packageOverview).filter(platform => platform !== Platform.common);

    return (
        <div className={styles.platformTags}>
            {platforms.map(platform => (
                <PlatformTag key={platform} className={styles.platformTag}>
                    {getPlatformName(platform)}
                </PlatformTag>
            ))}
        </div>
    );
}
