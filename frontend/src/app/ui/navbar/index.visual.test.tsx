import { render } from 'vitest-browser-react';
import { page, userEvent } from 'vitest/browser';
import { expect, test, vi } from 'vitest';

import { setNavigation } from '@/test/next-navigation';
import Navbar from './index';

vi.mock('@/app/api', () => import('@/test/api'));

const renderNavbar = (pathname: string) => {
    setNavigation({ pathname });
    return render(
        <div style={{ minHeight: 560 }}>
            <Navbar />
        </div>,
    );
};

test('navbar on the home page with the kotlin ecosystem menu open', async () => {
    await page.viewport(1280, 720);
    await renderNavbar('/');

    await page.getByTestId('ecosystem-menu-button').click();
    await expect.element(page.getByTestId('ecosystem-menu-links')).toBeVisible();
    // toBeVisible ignores opacity, and the menu fades in from opacity 0.
    await expect.poll(() => {
        const menu = page.getByTestId('ecosystem-menu-links').element().closest('[class*="modal"]');
        return menu ? getComputedStyle(menu).opacity : 'missing';
    }).toBe('1');
});

test('navbar on an inner page while typing a search', async () => {
    await page.viewport(1280, 720);
    await renderNavbar('/project/arrow-kt/arrow');

    await userEvent.type(page.getByRole('textbox').first(), 'ktor');
    await expect.element(page.getByText('Clear')).toBeVisible();
});

test('navbar mobile menu open', async () => {
    await page.viewport(390, 844);
    await renderNavbar('/');

    await page.getByTestId('mobile-menu-button').click();
    await expect.element(page.getByText('About & FAQ')).toBeVisible();
});
