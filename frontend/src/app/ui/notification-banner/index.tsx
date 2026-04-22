'use client'

import cn from "classnames";
import Image from "next/image";
import {Button} from "@rescui/button";
import {CloseIcon} from '@rescui/icons'
import {textCn} from '@rescui/typography';
import {ThemeProvider} from '@rescui/ui-contexts';
import {useRouter} from 'next/navigation';
import {useState, useCallback} from 'react';

import surveyIcon from './survey.png'

import styles from './style.module.css'

export default function NotificationBanner() {

    const NOTIFICATION_URL = 'https://surveys.jetbrains.com/s3/klibs-io-survey-klibs';

    const router = useRouter();
    const [notificationHidden, setNotificationHidden] = useState(false);

    const closeNotification = useCallback(async () => {
        try {
            setNotificationHidden(true);
            await fetch('/api/notification/close', {method: 'POST'});
            router.refresh();
        } catch (e) {
            setNotificationHidden(false);
        }
    }, [router]);

    if (notificationHidden) return null;

    return (
        <ThemeProvider theme={'dark'}>
            <div className={styles.wrapper}>
                <Image src={surveyIcon.src} width={48} height={48} alt={'Survey icon'} className={styles.icon}/>
                <a href={NOTIFICATION_URL} className={cn(textCn('rs-text-2', {hardness: 'hard'}), styles.text)}>
                    Complete survey to improve klibs.io ↗
                </a>
                <div className={styles.closeButtonWrapper}>
                    <Button
                        onClick={closeNotification}
                        theme={'dark'}
                        icon={<CloseIcon/>}
                        size={'l'}
                        mode={'clear'}
                        aria-label={'Close notification banner'}
                    />
                </div>
                <div className={styles.mobileButtonsWrapper}>
                    <a className={styles.mobileButton} href={NOTIFICATION_URL}>Let’s go</a>
                    <button onClick={closeNotification} className={styles.mobileButton}>Close</button>
                </div>
            </div>
        </ThemeProvider>
    );
}
