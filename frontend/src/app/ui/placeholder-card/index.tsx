import Link from "next/link";
import cn from 'classnames';
import { cardCn } from '@rescui/card';
import { textCn } from '@rescui/typography';

import styles from './styles.module.css';
import { PlusIcon } from "@rescui/icons";

export default function PlaceholderCard() {
    return (
        <Link
            data-testid="create-and-submit-project-link"
            href="/faq#how-do-i-add-a-project"
            className={cn(cardCn({ paddings: 16 }), styles.card)}
        >
            <div className={styles.cardContent}>
                <div className={styles.iconWrapper}>
                    <PlusIcon size={'l'} color={'#4D00FF'} />
                </div>
                <p className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.cardText)}>
                    Create and submit your own project
                </p>
            </div>

        </Link>
    );
}
