import React, {useEffect} from 'react';

import {Sidebar} from '@jetbrains/kotlin-web-site-ui/out/components/sidebar';
import {ThemeProvider} from '@rescui/ui-contexts';

import styles from './styles.module.css'

interface SidebarProps {
    isOpen: boolean;
    onClose: () => void;
    children?: React.ReactNode;
}

export default function SidebarMobile({isOpen, onClose, children}: SidebarProps) {

    useEffect(() => {
        if (isOpen) {
            document.documentElement.classList.add('scroll-lock');
        } else {
            document.documentElement.classList.remove('scroll-lock');
        }

        return () => {
            document.documentElement.classList.remove('scroll-lock');
        };
    }, [isOpen]);

    return (
        <ThemeProvider theme={'dark'}>
            <Sidebar
                isOpen={isOpen}
                onClose={onClose}
            >
                <div className={styles.scroller}>
                    {children}
                </div>
            </Sidebar>
        </ThemeProvider>
    )
}
