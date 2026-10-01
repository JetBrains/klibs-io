import { expect, vi } from 'vitest';

// Animations and transitions finish instantly rather than being removed: `animation: none` strands a
// fade-in at its invisible first frame.
export const visualCss = `
    *, *::before, *::after {
        animation-delay: 0s !important;
        animation-duration: 0s !important;
        animation-iteration-count: 1 !important;
        transition-delay: 0s !important;
        transition-duration: 0s !important;
    }
`;

// A requestAnimationFrame loop draws its first frame, then stops, so the canvas is the same every run.
export function renderSingleAnimationFrame() {
    let frames = 0;
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
        if (frames === 0) setTimeout(() => callback(0));
        frames += 1;
        return frames;
    });
}


// Some components (rescui tab indicators) move with JavaScript, which the CSS override cannot stop.
export async function waitForLayoutToSettle() {
    let previous = '';
    await expect.poll(() => {
        const current = Array.from(document.body.querySelectorAll('*'))
            .map((el) => { const r = el.getBoundingClientRect(); return `${r.x},${r.y},${r.width},${r.height}`; })
            .join(';');
        const settled = current === previous;
        previous = current;
        return settled;
    }, { interval: 150, timeout: 10_000 }).toBe(true);
}
