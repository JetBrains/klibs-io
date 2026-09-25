import Link from "next/link";
import { useEffect, useRef } from "react";
import {SearchIcon} from "@rescui/icons";

export default function SearchButton() {
    const linkRef = useRef<HTMLAnchorElement | null>(null);

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            const isMacOS = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
            const isShortcut = (isMacOS && event.metaKey) || (!isMacOS && event.ctrlKey);

            if (isShortcut && event.key === 'k') {
                event.preventDefault();
                linkRef.current?.click();
            }
        };

        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, []);

    return (
        <Link href={"/search"} className={'btn btn-primary'} ref={linkRef}>
            <SearchIcon size={'xs'} className="text-light pe-1"/>
            {" Search "}
            <span className="x-btn-hotkey">
                ⌘ + K
            </span>
        </Link>
    );
}