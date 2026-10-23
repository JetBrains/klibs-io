import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test } from 'vitest';

import CompareHint from './index';

describe('CompareHint', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    test('is shown to a first-time visitor', async () => {
        render(<CompareHint/>);

        expect(await screen.findByRole('heading', { name: 'Compare libraries' })).toBeInTheDocument();
    });

    test.each(['Got it', 'Close'])('stays hidden after being dismissed with "%s"', async (button) => {
        const { unmount } = render(<CompareHint/>);
        await userEvent.click(await screen.findByRole('button', { name: button }));
        expect(screen.queryByRole('heading', { name: 'Compare libraries' })).not.toBeInTheDocument();

        unmount();
        render(<CompareHint/>);

        expect(screen.queryByRole('heading', { name: 'Compare libraries' })).not.toBeInTheDocument();
    });
});
