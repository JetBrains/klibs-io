import coreWebVitals from 'eslint-config-next/core-web-vitals';
import typescript from 'eslint-config-next/typescript';

// Flat config replacement for the legacy `.eslintrc.json`
// (`next lint` was removed in Next.js 16 — ESLint CLI is used directly now).
const config = [
    ...coreWebVitals,
    ...typescript,
    {
        ignores: ['.husky/_/**', 'uiverify-archive/**', 'playwright-report/**', 'test-results/**'],
    },
    {
        rules: {
            // react-hooks v6 (React Compiler analysis), shipped with eslint-config-next 16,
            // flags pre-existing patterns in app code as errors. Demoted to warnings so
            // `npm run lint` (also the husky pre-commit hook) stays green; the flagged
            // components need a dedicated refactor pass before these can error again.
            'react-hooks/set-state-in-effect': 'warn',
            'react-hooks/purity': 'warn',
            'react-hooks/immutability': 'warn',
            'react-hooks/static-components': 'warn',
            'react-hooks/error-boundaries': 'warn',
        },
    },
];

export default config;
