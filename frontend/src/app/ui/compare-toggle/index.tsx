'use client'

import {KeyboardEvent, MouseEvent} from 'react';
import cn from 'classnames';
import {ToggleControl} from '@rescui/toggle';
import {textCn} from '@rescui/typography';

import {useCompare} from '@/app/ui/compare-provider';

import styles from './styles.module.css';

interface CompareToggleProps {
    ownerLogin: string;
    name: string;
}

export default function CompareToggle({ownerLogin, name}: CompareToggleProps) {
    const compare = useCompare();
    if (!compare) return null;

    const checked = compare.isSelected({ownerLogin, name});
    const disabled = !checked && compare.isFull;

    // Rendered inside the card link: cancel navigation and drive the checked state manually
    const onClick = (e: MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (!disabled) compare.toggle({ownerLogin, name});
    };

    // A native keyboard click would be cancelled by onClick and revert the checkbox, so toggle here instead.
    // Space also clicks on keyup in some browsers, which onKeyUp cancels
    const onKeyDown = (e: KeyboardEvent) => {
        if (e.key !== ' ' && e.key !== 'Enter') return;
        e.preventDefault();
        if (!disabled && !e.repeat) compare.toggle({ownerLogin, name});
    };

    return (
        <label className={styles.toggle} onClick={onClick} onKeyDown={onKeyDown}
               onKeyUp={e => e.key === ' ' && e.preventDefault()} onMouseDown={e => e.preventDefault()}>
            <span className={cn(textCn('rs-text-3'), {[styles.checkedLabel]: checked})}>
                {checked ? `Compare (${compare.items.length})` : 'Compare'}
            </span>
            <ToggleControl notUseLabelTag size={'m'} checked={checked} disabled={disabled} onChange={() => {}}/>
        </label>
    );
}
