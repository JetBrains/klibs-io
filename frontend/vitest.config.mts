import path from 'node:path';
import { playwright } from '@vitest/browser-playwright';
import { uiverifyPlugin } from '@uiverify/vitest/plugin';
import { defineConfig } from 'vitest/config';

export default defineConfig({
    oxc: {
        jsx: {
            runtime: 'automatic',
        },
    },
    optimizeDeps: {
        exclude: ['next/image', 'next/link'],
        // Declared explicitly so a run never triggers a mid-test re-optimize + page reload.
        include: [
            '@jetbrains/kotlin-web-site-ui/out/components/sidebar',
            '@jetbrains/kotlin-web-site-ui/out/components/sidebar-menu',
            '@jetbrains/kotlin-web-site-ui/out/components/typography',
            '@rescui/button',
            '@rescui/card',
            '@rescui/checkbox',
            '@rescui/chip-list',
            '@rescui/dropdown',
            '@rescui/dropdown-menu',
            '@rescui/focus-manager',
            '@rescui/icons',
            '@rescui/menu',
            '@rescui/radio-button',
            '@rescui/select',
            '@rescui/switcher',
            '@rescui/tab-list',
            '@rescui/table',
            '@rescui/tag',
            '@rescui/tooltip',
            '@rescui/typography',
            '@rescui/ui-contexts',
            'classnames',
            'date-fns',
            'next/navigation',
            'react',
            'react/jsx-dev-runtime',
            'react-remove-scroll-bar',
            'shiki',
            'vitest-browser-react',
        ],
    },
    resolve: {
        alias: {
            '@': path.resolve(import.meta.dirname, './src'),
        },
    },
    test: {
        env: {
            TZ: 'UTC',
        },
        // Keep per-project: `extends: true` concatenates arrays, so root globs/setup leak into every project.
        projects: [
            {
                extends: true,
                test: {
                    name: 'unit',
                    environment: 'jsdom',
                    exclude: ['src/**/*.visual.test.{ts,tsx}'],
                    include: ['src/**/*.test.{ts,tsx}'],
                    setupFiles: ['./src/test/setup.ts'],
                },
            },
            {
                extends: true,
                plugins: [uiverifyPlugin()],
                // Next inlines public env at build time; browser tests have no `process`.
                define: { 'process.env.NEXT_PUBLIC_API_URL': JSON.stringify('https://api.klibs.test') },
                test: {
                    name: 'visual',
                    browser: {
                        enabled: true,
                        headless: true,
                        provider: playwright(),
                        instances: [{ browser: 'chromium' }],
                    },
                    include: ['src/**/*.visual.test.{ts,tsx}'],
                    setupFiles: ['./src/test/visual-setup.ts'],
                    restoreMocks: true,
                    unstubGlobals: true,
                },
            },
        ],
    },
});
