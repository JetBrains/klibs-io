import {useEffect, useState} from "react";

export function useLocalStorage<T>(id: string, defaultValue: T) {
    const state = useState<T>(() => {
        if (typeof window === 'undefined') return defaultValue;

        let result: T | null = null;

        if (window.localStorage) {
            const storeValue = window.localStorage.getItem(id);
            if (storeValue) result = JSON.parse(storeValue);
        }

        return result || defaultValue;
    });

    const value = state[0];

    useEffect(() => {
        if (window?.localStorage) {
            if (!value) localStorage.removeItem(id);
            localStorage.setItem(id, JSON.stringify(value));
        }
    }, [id, value]);

    return state;
}


export function useWindowWidth() {
    const [windowWidth, setWindowWidth] = useState<number | undefined>(undefined);

    useEffect(() => {
        const handleResize = () => {
            setWindowWidth(window.innerWidth);
        };
        handleResize();
        window.addEventListener("resize", handleResize);
        return () => {
            window.removeEventListener("resize", handleResize);
        };
    }, []);

    return windowWidth;
}

export function useScrollPosition() {
    const [scrollPosition, setScrollPosition] = useState(0);

    useEffect(() => {
        const handleScroll = () => {
            setScrollPosition(window.scrollY);
        };

        handleScroll();

        window.addEventListener('scroll', handleScroll);

        return () => {
            window.removeEventListener('scroll', handleScroll);
        };
    }, []);

    return scrollPosition;
}

export function useScrollDirection() {
    const [scrollDirection, setScrollDirection] = useState<'up' | 'down' | null>(null);
    const [prevScrollY, setPrevScrollY] = useState(0);

    useEffect(() => {
        const handleScroll = () => {
            const currentScrollY = window.scrollY;

            if (currentScrollY > prevScrollY) {
                setScrollDirection('down');
            } else if (currentScrollY < prevScrollY) {
                setScrollDirection('up');
            }

            setPrevScrollY(currentScrollY);
        };

        window.addEventListener('scroll', handleScroll);

        return () => {
            window.removeEventListener('scroll', handleScroll);
        };
    }, [prevScrollY]);

    return scrollDirection;
}
