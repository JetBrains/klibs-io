import styles from './styles.module.css';
import React from "react";
import {RocketIcon, WinIcon} from "@rescui/icons";
import cn from "classnames";
import {textCn} from "@rescui/typography";

interface FeaturedLabelProps {
    isGrantWinner?: boolean;
    isFeaturedProject?: boolean;
}

export default function FeaturedLabel({isGrantWinner, isFeaturedProject}: FeaturedLabelProps) {
    return (
        <div className={cn(styles.featuredLabel, textCn("rs-text-2"), {[styles.isGrantWinner]: isGrantWinner, [styles.isFeatured]: isFeaturedProject})}>
            {isGrantWinner && <><WinIcon className={styles.featuredIcon}/> Kotlin grant winner</>}

            {isFeaturedProject && <><RocketIcon className={styles.featuredIcon}/> Featured project</>}
        </div>
    );
}