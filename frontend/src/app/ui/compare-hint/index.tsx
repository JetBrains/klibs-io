'use client'

import {useEffect, useState} from 'react';
import cn from 'classnames';
import {Button} from '@rescui/button';
import {CloseOutlineIcon} from '@rescui/icons';
import {createTextCn} from '@rescui/typography';
import {ThemeProvider} from '@rescui/ui-contexts';

import styles from './styles.module.css';

const DISMISSED_KEY = 'compareHintDismissed';
const darkTextCn = createTextCn('dark');

export default function CompareHint() {
    // Hidden until mounted so the server render matches and dismissed users never see a flash
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        setVisible(window.localStorage.getItem(DISMISSED_KEY) === null);
    }, []);

    if (!visible) return null;

    const dismiss = () => {
        window.localStorage.setItem(DISMISSED_KEY, 'true');
        setVisible(false);
    };

    return (
        <ThemeProvider theme={'dark'}>
            <aside className={styles.hint} aria-labelledby="compare-hint-title">
                <div className={styles.text}>
                    <h2 id="compare-hint-title" className={cn(darkTextCn('rs-h4'), styles.title)}>Compare libraries</h2>
                    <p className={cn(darkTextCn('rs-text-3', {hardness: 'hard'}), styles.description)}>
                        Add libraries to see key differences side by side.
                    </p>
                </div>
                <Button size={'xs'} mode={'rock'} onClick={dismiss}>Got it</Button>
                <button type="button" className={styles.close} aria-label="Close" onClick={dismiss}>
                    <CloseOutlineIcon size={'s'}/>
                </button>
            </aside>
        </ThemeProvider>
    );
}
