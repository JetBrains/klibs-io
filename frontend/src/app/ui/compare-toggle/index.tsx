'use client'

import {MouseEvent} from 'react';
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

    return (
        <label className={styles.toggle} onClick={onClick} onMouseDown={e => e.preventDefault()}>
            <span className={cn(textCn('rs-text-3'), {[styles.checkedLabel]: checked})}>
                {checked ? `Compare (${compare.items.length})` : 'Compare'}
            </span>
            <ToggleControl notUseLabelTag size={'m'} checked={checked} disabled={disabled} onChange={() => {}}/>
        </label>
    );
}
