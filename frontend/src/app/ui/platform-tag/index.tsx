import { Tag } from '@rescui/tag';

interface PlatformTagProps {
    children: React.ReactNode;
    className?: string;
}

export default function PlatformTag({ children, className }: PlatformTagProps) {
    return (
        <Tag
            style={{ color: "#19191CB2", borderColor: "#00000033" }}
            className={className}
        >
            {children}
        </Tag>
    );
}
