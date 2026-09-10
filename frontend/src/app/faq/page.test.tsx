import { render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';

import Faq from './page';

vi.mock('@rescui/icons', () => ({
    SlackIcon: () => null,
}));

describe('FAQ page', () => {
    test('provides the primary submission and support destinations', () => {
        render(<Faq />);

        expect(screen.getByRole('link', { name: 'submit an indexing request' }))
            .toHaveAttribute('href', expect.stringContaining('template=index_request.yml'));
        expect(screen.getByRole('link', { name: 'GitHub issue tracker' }))
            .toHaveAttribute('href', 'https://github.com/JetBrains/klibs-io/issues/new/choose');
        expect(screen.getByRole('link', { name: /#klibs-io channel/ }))
            .toHaveAttribute('href', 'https://kotlinlang.slack.com/archives/C081AF4JK70');
    });
});
