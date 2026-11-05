import { ReactNode } from "react";
import cn from 'classnames';
import { textCn } from '@rescui/typography';

import styles from './styles.module.css';

export interface CardDetail {
    label: string;
    value: ReactNode;
}

interface CardDetailsProps {
    rows: CardDetail[];
    className?: string;
}

export default function CardDetails({ rows, className }: CardDetailsProps) {
    return (
        <dl className={cn(styles.details, className)}>
            {rows.map(({ label, value }) => (
                <div key={label} className={styles.row}>
                    <dt className={cn(textCn('rs-text-3', { hardness: 'pale' }), styles.label)}>{label}</dt>
                    <dd className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.value)}>{value}</dd>
                </div>
            ))}
        </dl>
    );
}
