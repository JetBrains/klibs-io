'use client'

import {Button} from '@rescui/button';
import {RemovableTag} from '@rescui/tag';

import styles from './styles.module.css';

export interface CompareBarItem {
    id: number;
    ownerLogin: string;
    name: string;
}

interface CompareBarProps {
    items: CompareBarItem[];
    maxItems: number;
    onRemove: (id: number) => void;
    onClear: () => void;
    onCompare: () => void;
    showCompare?: boolean;
}

export default function CompareBar(
    {items, maxItems, onRemove, onClear, onCompare, showCompare = true}: CompareBarProps
) {
    return (
        <div className={styles.compareBar}>
            <ul className={styles.items}>
                {items.map(item => (
                    <li key={item.id}>
                        <RemovableTag size={'xs'} onRemoveClick={() => onRemove(item.id)}>
                            {item.name}
                        </RemovableTag>
                    </li>
                ))}
            </ul>
            <div className={styles.actions}>
                <Button size={'s'} mode={'outline'} onClick={onClear}>{showCompare ? 'Clear' : 'Clear all'}</Button>
                {showCompare &&
                    <Button size={'s'} disabled={items.length < 2} onClick={onCompare}>
                        Compare ({items.length}/{maxItems})
                    </Button>
                }
            </div>
        </div>
    );
}
