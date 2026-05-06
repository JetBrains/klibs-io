import React from 'react'
import cn from 'classnames';

import styles from './styles.module.css'

interface Props {
    onClick: () => void
    title?: string
    icon?: React.ReactNode
    counter?: number
    isMobile?: boolean
    'data-testid'?: string
    compact?: boolean
}

export function DropdownTrigger({title, icon, onClick, counter, isMobile, 'data-testid': dataTestId, compact}: Props) {
    return (
        <button
            onClick={onClick}
            className={cn(styles.dropdownTriggerWrapper, {
                [styles.dropdownTriggerMobile]: isMobile,
                [styles.dropdownTriggerCompact]: compact
            })}
            data-testid={dataTestId}
        >
            {title && title}{icon}{counter ?
            <div className={styles.counter}>{counter}</div> : null}
        </button>
    )
}
