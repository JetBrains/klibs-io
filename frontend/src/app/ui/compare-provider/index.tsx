'use client'

import {createContext, ReactNode, useCallback, useContext, useMemo, useState} from 'react';
import {useRouter} from 'next/navigation';

import CompareBar, {CompareBarItem} from '@/app/ui/compare-bar';
import {MAX_COMPARE_ITEMS} from '@/app/constants';
import {getCompareLink} from '@/app/helpers';

import styles from './styles.module.css';

interface CompareContextValue {
    items: CompareBarItem[];
    isFull: boolean;
    isSelected: (id: number) => boolean;
    toggle: (item: CompareBarItem) => void;
}

const CompareContext = createContext<CompareContextValue | undefined>(undefined);

export function useCompare(): CompareContextValue | undefined {
    return useContext(CompareContext);
}

export default function CompareProvider({children}: { children: ReactNode }) {
    const router = useRouter();
    const [items, setItems] = useState<CompareBarItem[]>([]);

    const remove = useCallback((id: number) => setItems(current => current.filter(item => item.id !== id)), []);

    const toggle = useCallback((item: CompareBarItem) => setItems(current => {
        if (current.some(it => it.id === item.id)) return current.filter(it => it.id !== item.id);
        return current.length < MAX_COMPARE_ITEMS ? [...current, item] : current;
    }), []);

    const value = useMemo(() => ({
        items,
        isFull: items.length >= MAX_COMPARE_ITEMS,
        isSelected: (id: number) => items.some(item => item.id === id),
        toggle,
    }), [items, toggle]);

    const compareLink = getCompareLink(items.map(it => ({ownerLogin: it.ownerLogin, projectName: it.name})));

    return (
        <CompareContext.Provider value={value}>
            {children}
            {items.length > 0 &&
                <div className={styles.barWrapper}>
                    <CompareBar
                        items={items}
                        maxItems={MAX_COMPARE_ITEMS}
                        onRemove={remove}
                        onClear={() => setItems([])}
                        onCompare={() => router.push(compareLink)}
                    />
                </div>
            }
        </CompareContext.Provider>
    );
}
