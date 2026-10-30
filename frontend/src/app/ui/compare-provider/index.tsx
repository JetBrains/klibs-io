'use client'

import {createContext, ReactNode, useCallback, useContext, useMemo, useRef, useState} from 'react';
import {usePathname, useRouter} from 'next/navigation';

import CompareBar, {CompareBarItem} from '@/app/ui/compare-bar';
import CompareHint from '@/app/ui/compare-hint';
import {MAX_COMPARE_ITEMS} from '@/app/constants';
import {getCompareLink} from '@/app/helpers';

import styles from './styles.module.css';

interface CompareContextValue {
    items: CompareBarItem[];
    isFull: boolean;
    isSelected: (item: CompareBarItem) => boolean;
    toggle: (item: CompareBarItem) => void;
    replace: (items: CompareBarItem[]) => void;
}

const isSame = (a: CompareBarItem, b: CompareBarItem) => a.ownerLogin === b.ownerLogin && a.name === b.name;

const toCompareLink = (items: CompareBarItem[]) =>
    getCompareLink(items.map(it => ({ownerLogin: it.ownerLogin, projectName: it.name})));

const PROJECT_PATH = /^\/project\/([^/]+)\/([^/]+)$/;

const toProjectItem = (pathname: string): CompareBarItem | undefined => {
    const match = PROJECT_PATH.exec(pathname);
    if (!match) return undefined;
    try {
        return {ownerLogin: decodeURIComponent(match[1]), name: decodeURIComponent(match[2])};
    } catch {
        return undefined;
    }
};

const CompareContext = createContext<CompareContextValue | undefined>(undefined);

export function useCompare(): CompareContextValue | undefined {
    return useContext(CompareContext);
}

export default function CompareProvider({children}: { children: ReactNode }) {
    const router = useRouter();
    const pathname = usePathname();
    const [items, setItems] = useState<CompareBarItem[]>([]);
    const returnUrl = useRef<string>();

    const toggle = useCallback((item: CompareBarItem) => setItems(current => {
        if (current.some(it => isSame(it, item))) return current.filter(it => !isSame(it, item));
        return current.length < MAX_COMPARE_ITEMS ? [...current, item] : current;
    }), []);

    const replace = useCallback((next: CompareBarItem[]) => setItems(next.slice(0, MAX_COMPARE_ITEMS)), []);

    const value = useMemo(() => ({
        items,
        isFull: items.length >= MAX_COMPARE_ITEMS,
        isSelected: (item: CompareBarItem) => items.some(it => isSame(it, item)),
        toggle,
        replace,
    }), [items, toggle, replace]);

    const isComparePage = pathname === '/compare';
    // Query variants (?categories=, ?search=) share the '/' pathname
    const isMainPage = pathname === '/';
    const currentProject = toProjectItem(pathname);
    const canAddCurrent = currentProject !== undefined && !value.isSelected(currentProject);
    const clear = () => {
        setItems([]);
        if (isComparePage) router.push('/');
    };

    const compare = () => {
        returnUrl.current = window.location.pathname + window.location.search;
        router.push(toCompareLink(items));
    };

    // On the compare page the table follows the URL; replaceState updates it without a server round-trip
    const remove = (item: CompareBarItem) => {
        const next = items.filter(it => !isSame(it, item));
        setItems(next);
        if (!isComparePage) return;
        if (next.length > 1) window.history.replaceState(null, '', toCompareLink(next));
        else router.push('/');
    };

    return (
        <CompareContext.Provider value={value}>
            {children}
            {isMainPage && items.length === 0 && <CompareHint/>}
            {items.length > 0 &&
                <div className={styles.barWrapper}>
                    <CompareBar
                        items={items}
                        maxItems={MAX_COMPARE_ITEMS}
                        onRemove={remove}
                        onClear={clear}
                        onCompare={compare}
                        onAdd={canAddCurrent ? () => toggle(currentProject) : undefined}
                        onEditSelection={isComparePage ? () => router.push(returnUrl.current ?? '/') : undefined}
                        showCompare={!isComparePage}
                    />
                </div>
            }
        </CompareContext.Provider>
    );
}
