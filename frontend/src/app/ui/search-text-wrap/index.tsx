import { Fragment, ReactNode, useMemo } from "react";

import styles from './styles.module.css';

interface SearchTextWrapProps {
    search?: string;
    children?: string | null;
}

// Highlights every case-insensitive occurrence of the search query in the text
export default function SearchTextWrap({ search, children }: SearchTextWrapProps) {
    return useMemo(() => {
        if (!search || !children) return children;

        const chunks: ReactNode[] = [];
        let text = children;
        const query = search.toLowerCase();

        do {
            const i = text.toLowerCase().indexOf(query);

            if (i === -1) {
                text && chunks.push(<Fragment key={i}>{text}</Fragment>);
                break
            }

            const item = text.substring(0, i);
            const suffixIndex = i + query.length;

            chunks.push(<Fragment key={i + '-' + text.length}>{item}<span
                className={styles.highlight}>{text.substring(i, suffixIndex)}</span></Fragment>);
            text = text.substring(suffixIndex);
        } while (true);

        if (chunks.length === 1) return chunks[0];
        return <>{chunks}</>
    }, [search, children]);
}
