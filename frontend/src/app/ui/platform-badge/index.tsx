import {getPlatformName, Platform} from "@/app/types";
import cn from "classnames";

type PlatformBadgeSize = 'sm' | 'xxs';

export default function PlatformBadge({platform, size}: {platform: Platform, size?: PlatformBadgeSize}) {
    return (
        <span className={cn(`badge platform-${platform} ${size ? 'platform-badge-' + size : 'platform-badge'}`)}>
            {size == 'xxs' ? ' ' : getPlatformName(platform)}
        </span>
    )
}