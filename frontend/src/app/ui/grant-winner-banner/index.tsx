import React from "react";

import styles from './styles.module.css';
import {textCn} from "@rescui/typography";
import {Button} from "@rescui/button";
import Image from "next/image";
import kodeeGrantWinner from "./kodee-grant-winner.svg";
import cn from "classnames";
import {BackgroundSquares} from "./background-squares";

interface GrantWinnerBannerProps {
    categorySlug: string;
}

export default function GrantWinnerBanner({ categorySlug }: GrantWinnerBannerProps) {
    return (
        <div className={styles.grantWinnerBanner} data-testid="grant-winner-banner">
            <BackgroundSquares />
            <div className={styles.content}>
                <h2 className={cn(styles.title, textCn("rs-h1"))}>Kotlin Grant Winners</h2>

                <p className={cn(styles.text, textCn("rs-text-2"))}>
                    Explore top-tier projects funded for pushing Multiplatform development forward.
                </p>

                <div className={cn(styles.buttonsBlock)}>
                    <Button href={`/?category=${categorySlug}`} data-testid="grant-winners-discover-button">Discover</Button>
                    <Button href={'https://kotlinfoundation.org/grants/'} mode={'outline'} data-testid="grant-winners-submit-button">Learn about grants</Button>
                </div>
            </div>

            <div className={styles.imageContainer}>
                <div className={styles.imageWrapper}>
                    <Image src={kodeeGrantWinner} alt="Kodee grant winner image" fill={true} priority={true} />
                </div>
            </div>


        </div>
    );
}
