"use client"

import React, {useEffect, useRef, useState} from "react";
import cn from "classnames";

import {textCn} from "@rescui/typography";

import styles from "./styles.module.css";

interface TocNavItem {
    id: string;
    label: string;
}

interface TocNavProps {
    items: TocNavItem[];
    offset?: number;
}

const TocNav: React.FC<TocNavProps> = ({items, offset = 120}) => {
    const [activeId, setActiveId] = useState(items[0]?.id);
    const lastId = useRef(items[0]?.id);

    useEffect(() => {
        const sections = items
            .map((item) => document.getElementById(item.id))
            .filter((el): el is HTMLElement => el !== null);

        const updateActive = () => {
            const atBottom =
                window.innerHeight + window.scrollY >= document.body.scrollHeight - 2;

            let currentId = sections[0]?.id;
            if (atBottom) {
                currentId = sections[sections.length - 1]?.id;
            } else {
                for (const section of sections) {
                    if (section.getBoundingClientRect().top <= offset) {
                        currentId = section.id;
                    }
                }
            }

            if (currentId && currentId !== lastId.current) {
                lastId.current = currentId;
                setActiveId(currentId);
                history.replaceState(null, "", `#${currentId}`);
            }
        };

        updateActive();
        window.addEventListener("scroll", updateActive, {passive: true});
        window.addEventListener("resize", updateActive);
        return () => {
            window.removeEventListener("scroll", updateActive);
            window.removeEventListener("resize", updateActive);
        };
    }, [items, offset]);

    return (
        <nav className={styles.anchorNav}>
            <ul className={styles.anchorNavList}>
                {items.map((item) => (
                    <li key={item.id}>
                        <a
                            href={`#${item.id}`}
                            className={cn(
                                textCn('rs-text-3', {hardness: 'hard'}),
                                styles.anchorNavLink,
                                activeId === item.id && styles.anchorNavLinkActive
                            )}
                        >
                            {item.label}
                        </a>
                    </li>
                ))}
            </ul>
        </nav>
    );
};

export default TocNav;
