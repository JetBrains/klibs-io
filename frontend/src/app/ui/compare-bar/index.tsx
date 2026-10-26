'use client'

import {Button} from '@rescui/button';
import {PlusIcon} from '@rescui/icons';
import {RemovableTag} from '@rescui/tag';

import styles from './styles.module.css';

export interface CompareBarItem {
    ownerLogin: string;
    name: string;
}

interface CompareBarProps {
    items: CompareBarItem[];
    maxItems: number;
    onRemove: (item: CompareBarItem) => void;
    onClear: () => void;
    onCompare: () => void;
    onAdd?: () => void;
    onEditSelection?: () => void;
    showCompare?: boolean;
}

export default function CompareBar(
    {items, maxItems, onRemove, onClear, onCompare, onAdd, onEditSelection, showCompare = true}: CompareBarProps
) {
    return (
        <div className={styles.compareBar}>
            <ul className={styles.items}>
                {items.map(item => (
                    <li key={`${item.ownerLogin}/${item.name}`}>
                        <RemovableTag size={'xs'} onRemoveClick={() => onRemove(item)}>
                            {item.name}
                        </RemovableTag>
                    </li>
                ))}
            </ul>
            <div className={styles.actions}>
                <Button size={'s'} mode={'outline'} onClick={onClear}>Clear all</Button>
                {onAdd &&
                    <Button size={'s'} mode={'outline'} icon={<PlusIcon/>} disabled={items.length >= maxItems}
                            onClick={onAdd} aria-label={'Add to comparison'}>
                        <span>Add<span className={styles.longLabel}> to comparison</span></span>
                    </Button>
                }
                {showCompare &&
                    <Button size={'s'} disabled={items.length < 2} onClick={onCompare}>
                        Compare ({items.length}/{maxItems})
                    </Button>
                }
                {onEditSelection &&
                    <Button size={'s'} mode={'outline'} onClick={onEditSelection}>Edit selection</Button>
                }
            </div>
        </div>
    );
}
