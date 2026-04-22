import {useRef, useEffect} from "react";
import {MenuItem} from '@rescui/dropdown-menu';
import styles from './styles.module.css'
import {Tag, presets} from '@rescui/tag';
import {ProjectSearchResults} from "@/app/types";
import {useCallback} from "react";

interface SearchSuggestionsListProps {
    list: ProjectSearchResults[] | null | undefined
    onEnter?: (value: string) => void;
    suggestionsClose?: () => void;
}

export default function SearchSuggestionsList({list, onEnter, suggestionsClose}: SearchSuggestionsListProps) {

    const ref = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        const handleOutSideClick = (event: MouseEvent) => {
            if (!ref.current?.contains(event.target as Node)) {
                suggestionsClose && suggestionsClose();
            }
        };

        window.addEventListener("mousedown", handleOutSideClick);

        return () => {
            window.removeEventListener("mousedown", handleOutSideClick);
        };
    }, [ref, suggestionsClose]);

    const handleKeyPress = useCallback((e: React.KeyboardEvent<Element>, keyword: string) => {
        if (e.key === 'Enter') {
            onEnter && onEnter(keyword);
        }
    }, [onEnter])

    const handleClick = useCallback((keyword: string) => {
        onEnter && onEnter(keyword);
        suggestionsClose && suggestionsClose();
    }, [onEnter, suggestionsClose])

    const tagPreset = presets['filled-dark'];

    return (
        <div className={styles.list} ref={ref}>
            {list?.map(item => (
                <MenuItem
                    onKeyDown={(event) => handleKeyPress(event, item.name)}
                    onClick={() => handleClick(item.name)}
                    key={`${item.name}-${item.id}`}
                    iconPlacement={'right'}
                    className={styles.menuItem}
                    icon={<Tag className={styles.tag} {...tagPreset}>Enter</Tag>}
                >
                    {item.name}
                </MenuItem>
            ))}

        </div>
    )
}
