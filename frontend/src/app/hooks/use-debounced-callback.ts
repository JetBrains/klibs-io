import { useEffect, useMemo, useRef } from "react";

export type DebouncedCallback<A extends unknown[]> = ((...args: A) => void) & {
    cancel: () => void;
};

// Debounce a callback while always invoking its latest version,
// so callers can close over fresh props/state without stale values.
// cancel() drops a pending invocation (also done automatically on unmount).
export function useDebouncedCallback<A extends unknown[]>(
    callback: (...args: A) => void,
    delayMs: number
): DebouncedCallback<A> {
    const callbackRef = useRef(callback);
    useEffect(() => {
        callbackRef.current = callback;
    });

    const timerRef = useRef<number | null>(null);

    const debounced = useMemo(() => {
        const cancel = () => {
            if (timerRef.current !== null) {
                clearTimeout(timerRef.current);
                timerRef.current = null;
            }
        };
        const fn = (...args: A) => {
            cancel();
            timerRef.current = window.setTimeout(() => callbackRef.current(...args), delayMs);
        };
        fn.cancel = cancel;
        return fn as DebouncedCallback<A>;
    }, [delayMs]);

    useEffect(() => {
        return () => debounced.cancel();
    }, [debounced]);

    return debounced;
}
