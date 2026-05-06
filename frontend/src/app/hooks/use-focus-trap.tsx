import { useEffect, useRef } from 'react';

export function useFocusTrap(ref: React.RefObject<HTMLElement>, isActive: boolean, onEscape?: () => void) {
  const previouslyFocusedElement = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!isActive || !ref.current) return;

    const container = ref.current;

    previouslyFocusedElement.current = document.activeElement as HTMLElement;

    const selectors = 'a[href], button, textarea, input, select, [tabindex]:not([tabindex="-1"])';
    const focusableElements = Array.from(container.querySelectorAll<HTMLElement>(selectors));
    const first = focusableElements[0];
    const last = focusableElements[focusableElements.length - 1];

    first?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Tab') {
        if (focusableElements.length === 0) return;

        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }

      if (e.key === 'Escape') {
        onEscape?.();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previouslyFocusedElement.current?.focus();
    };
  }, [isActive, ref, onEscape]);
}
