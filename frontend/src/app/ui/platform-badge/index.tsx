import {getTargetGroupName, getTargetGroupPlatform} from "@/app/types";
import cn from "classnames";

type PlatformBadgeSize = 'sm' | 'xxs';

export default function PlatformBadge({group, size}: {group: string, size?: PlatformBadgeSize}) {
    const platform = getTargetGroupPlatform(group);

    return (
        <span className={cn(`badge platform-${platform} ${size ? 'platform-badge-' + size : 'platform-badge'}`)}>
            {size == 'xxs' ? ' ' : getTargetGroupName(group)}
        </span>
    )
}
