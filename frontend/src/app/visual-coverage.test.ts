// @vitest-environment node
import fs from 'node:fs';
import path from 'node:path';
import { expect, test } from 'vitest';

const srcDir = path.resolve(import.meta.dirname, '..');
const appDir = path.join(srcDir, 'app');
const ROUTE_FILE = /^(page|loading|error|not-found)\.tsx$/;

// The project's own .tsx modules a file imports, via `@/` or a relative path.
function importedComponents(file: string): string[] {
    return Array.from(fs.readFileSync(file, 'utf8').matchAll(/\bfrom ['"]([^'"]+)['"]/g), ([, spec]) => spec)
        .filter((spec) => spec.startsWith('@/') || spec.startsWith('.'))
        .map((spec) => (spec.startsWith('@/')
            ? path.join(srcDir, spec.slice(2))
            : path.resolve(path.dirname(file), spec)))
        .flatMap((base) => [`${base}.tsx`, path.join(base, 'index.tsx')])
        .filter((candidate) => fs.existsSync(candidate));
}

// A route counts as covered when a visual test renders its file or a component the file renders.
test('every page and route state has a visual test', () => {
    const files = fs.readdirSync(appDir, { recursive: true, encoding: 'utf8' }).map((f) => path.join(appDir, f));
    const rendered = new Set(files.filter((f) => f.endsWith('.visual.test.tsx')).flatMap(importedComponents));

    const untested = files
        .filter((f) => ROUTE_FILE.test(path.basename(f)))
        .filter((route) => ![route, ...importedComponents(route)].some((f) => rendered.has(f)))
        .map((route) => path.relative(appDir, route));

    expect(untested).toEqual([]);
});
