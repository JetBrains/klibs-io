import { formatDistanceToNow, formatDistanceToNowStrict, format } from 'date-fns';

interface Props {
    timestamp: number;
    strictMode?: boolean;
}

interface TimestampToDateProps {
    timestamp: number;
}

export function TimestampToDate({ timestamp }: TimestampToDateProps) {
    const date = new Date(timestamp);
    const formattedDate = format(date, 'dd MMM yyyy');
    return (
        <>{formattedDate}</>
    );
}

export default function TimeAgo({ timestamp, strictMode = false }: Props) {
    const date = new Date(timestamp);

    if (strictMode) {
        return (
            <>{formatDistanceToNowStrict(date, {addSuffix: true})}</>
        );
    } else {
        return (
            <>{formatDistanceToNow(date, {addSuffix: true})}</>
        );
    }
}
