import { useState } from 'react';
import cn from 'classnames';

import styles from './styles.module.css';
import { DropdownMenu, MenuItem } from '@rescui/dropdown-menu';


import { textCn } from '@rescui/typography'

interface SearchTopBarProps {
}

export default function SearchTopBar({ }: PlatformTagProps) {

    const [isOpen, setIsOpen] = useState(false);

    const toggleIsOpen = () => setIsOpen(s => !s);

    return (
        <div className={styles.wrapper}>
            <div className={textCn('rs-h4')}>Results: (count)</div>
            <div className={cn(textCn('rs-text-2', { hardness: 'hard' }), styles.sort)}>
                <div>Sort by&nbsp;</div>

                <DropdownMenu
                    isOpen={isOpen}
                    onRequestClose={() => setIsOpen(false)}
                    trigger={<div onClick={toggleIsOpen} className={styles.trigger}>Github stars</div>}
                >
                    <MenuItem disabled>Github stars</MenuItem>
                    <MenuItem>Relevance</MenuItem>
                    <MenuItem>OSS Health</MenuItem>
                    <MenuItem>Dependents</MenuItem>
                </DropdownMenu>
            </div>
        </div>
    );
}
