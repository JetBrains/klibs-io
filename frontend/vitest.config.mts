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
            '@rescui/card',
            '@rescui/icons',
            '@rescui/tag',
            '@rescui/typography',
            'classnames',
            'react',
            'react/jsx-dev-runtime',
            'vitest-browser-react',
        ],
    },
    resolve: {
        alias: {
            '@': path.resolve(import.meta.dirname, './src'),
        },
    },
    test: {
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
                test: {
                    name: 'visual',
                    browser: {
                        enabled: true,
                        headless: true,
                        provider: playwright(),
                        instances: [{ browser: 'chromium' }],
                    },
                    include: ['src/**/*.visual.test.{ts,tsx}'],
                },
            },
        ],
    },
});
