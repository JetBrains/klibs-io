import { ComponentPropsWithoutRef } from 'react';

export default function NextLink({ children, ...props }: ComponentPropsWithoutRef<'a'>) {
    return <a {...props}>{children}</a>;
}
