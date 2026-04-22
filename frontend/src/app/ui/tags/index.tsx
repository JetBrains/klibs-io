import cn from "classnames";
import React from "react";
import styles from './styles.module.css';
import { textCn } from "@rescui/typography";

interface TagsProps {
    className?: string;
    tags: string[];
}

export default function Tags({ className, tags }: TagsProps) {
    return (
        <ul className={cn(styles.row, textCn("rs-text-3"),className)}>
            {tags.map((tag,) => (
                <li key={tag} className={styles.tag}><a>#{tag}</a></li>
            ))}
        </ul>
    );
}