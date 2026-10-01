// Same stylesheet order as the root layout, so captures cascade the way production does.
import '@/app/globals.css';
import 'bootstrap/dist/css/bootstrap.css';
import '@rescui/typography/lib/font-jb-sans-auto.css';

import { afterEach, vi } from 'vitest';

import { visualCss } from './visual';

vi.mock('next/image', () => import('./next-image'));
vi.mock('next/link', () => import('./next-link'));
vi.mock('next/navigation', () => import('./next-navigation'));

const style = document.createElement('style');
style.textContent = visualCss;
document.head.append(style);

afterEach(() => {
    vi.useRealTimers();
});
