import {useEffect, useRef, useState} from 'react';
import styles from './styles.module.css';
import cn from 'classnames';

import {Button} from "@rescui/button";
import {textCn} from '@rescui/typography';

import Image from "next/image";
import Link from "next/link";

import {RemoveScrollBar} from 'react-remove-scroll-bar';

import {ArrowRightIcon, MoreIcon} from "@rescui/icons";
import KtDocsIcon from "@/app/img/kt-menu-docs.svg";
import KtPlayIcon from "@/app/img/kt-menu-play.svg";

import {kotlinEcosystemMenu} from "@/app/ui/kotlin-ecosystem-dropdown/nav-scheme";

import {useFocusTrap} from "@/app/hooks/use-focus-trap";

import {trackEvent, GAEvent} from "@/app/analytics";

export function KotlinEcosystemDropdown() {
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const modalRef = useRef<HTMLDivElement | null>(null);
    const triggerRef = useRef<HTMLButtonElement | null>(null);

    const handleCloseModal = () => setIsOpen(false);
    const handleToggleModal = () => {
        setIsOpen(!isOpen);
    }

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                handleCloseModal();
            }
        };

        const handleClickOutside = (e: MouseEvent) => {
            if (triggerRef.current && triggerRef.current.contains(e.target as Node)) {
                return;
            }

            if (modalRef.current && !modalRef.current.contains(e.target as Node)) {
                handleCloseModal();
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleKeyDown);
            document.addEventListener('mousedown', handleClickOutside);
            document.documentElement.classList.add('scroll-lock');
        }

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            document.removeEventListener('mousedown', handleClickOutside);
            document.documentElement.classList.remove('scroll-lock');
        };
    }, [isOpen]);

    useFocusTrap(modalRef, isOpen, handleCloseModal);

    return (
        <div className={styles.ktEcosystemMenuWrapper}>
            <Button
                ref={triggerRef}
                className={styles.triggerIcon}
                onClick={() => {handleToggleModal(); trackEvent(GAEvent.KOTLIN_ECOSYSTEM_DROPDOWN_CLICK, {})}}
                content={"icon"}
                icon={<MoreIcon />}
                mode={"clear"}
                data-testid="ecosystem-menu-button"
            />

            {isOpen && (
                <>
                    <RemoveScrollBar gapMode={'padding'} />
                    <div className={styles.backdrop}>
                        <div ref={modalRef}
                             className={cn(styles.modal, cn(textCn('rs-text-2'), styles.ktEcosystemDropdown))}>
                            <div className={styles.ktMenuMainContainer}>
                                {/*Purple container*/}
                                <div className={styles.ktMenuPurpleContainer} data-testid="ecosystem-menu-main-links">
                                    {/*Kotlin docs*/}
                                    <Link href="https://kotlinlang.org/docs/home.html"
                                          className={cn(styles.ktMenuDocs, styles.ktKotlinContainer)}
                                    >
                                        <Image width={48} height={48} src={KtDocsIcon} alt="Docs icon"/>
                                        <h3 className={cn(textCn("rs-h3"), styles.ktMenuTitle)}>Kotlin <br/> documentation
                                        </h3>
                                        <div className={styles.ktMenuArrowContainer}>
                                            <ArrowRightIcon/>
                                        </div>
                                    </Link>

                                    {/*Kotlin playground*/}
                                    <Link
                                        href="https://play.kotlinlang.org/"
                                        className={cn(styles.ktMenuPlay, styles.ktKotlinContainer)}
                                    >
                                        <Image width={48} height={48} src={KtPlayIcon} alt="Play icon"/>
                                        <h3 className={cn(textCn("rs-h3"), styles.ktMenuTitle)}>Kotlin <br/> playground
                                        </h3>
                                        <div className={styles.ktMenuArrowContainer}>
                                            <ArrowRightIcon/>
                                        </div>
                                    </Link>
                                </div>

                                {/*White container*/}
                                <div className={styles.ktMenuWhiteContainer} data-testid="ecosystem-menu-links">
                                    {kotlinEcosystemMenu.filter(item => item.items !== undefined).map((item, index) => (
                                        <div key={index} className={styles.ktMenuLinkContainer}>
                                            <h3 className={cn(textCn('rs-h5'), styles.ktMenuHeading)}>{item.title}</h3>
                                            {item.items.map((item, index) => (
                                                <Link
                                                    key={index}
                                                    href={item.url}
                                                    className={cn(styles.ktMenuLink)}
                                                >
                                                    {item.title}
                                                </Link>
                                            ))}
                                        </div>
                                    ))}

                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}
