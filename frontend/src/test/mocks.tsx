import { ReactNode } from 'react';
import { vi } from 'vitest';

vi.mock('next/image', () => ({
    default: ({ alt }: { alt: string }) => <span aria-label={alt} />,
}));

vi.mock('@rescui/typography', () => ({
    textCn: () => '',
}));

vi.mock('@/app/ui/container', () => ({
    default: ({ children, dataTestId }: { children: ReactNode; dataTestId?: string }) => (
        <div data-testid={dataTestId}>{children}</div>
    ),
}));
