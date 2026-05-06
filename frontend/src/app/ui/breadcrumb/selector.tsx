import {MouseEvent, useState} from "react";
import cn from "classnames";
import {useRouter} from "next/navigation";

import {DownIcon, LoadingIcon} from "@rescui/icons";
import {Button, ButtonProps} from "@rescui/button";
import {DropdownMenu} from "@rescui/dropdown-menu";
import {MenuItem, MenuItemProps} from "@rescui/menu";

import styles from './selector.module.css'

export type BreadcrumbsSelectorProps = {
    current: ButtonProps['children'],
    menu?: (Required<Pick<MenuItemProps, 'href' | 'children'>> & MenuItemProps)[]
}

export function BreadcrumbsSelector({pathname, current, menu}: BreadcrumbsSelectorProps & { pathname?: string }) {
    const [isOpen, setIsOpen] = useState(false);
    const router = useRouter();

    const isLoading = !menu || menu.length === 0;

    if (isLoading) {
        return <>{current}&nbsp;<LoadingIcon/></>
    }

    const toggleIsOpen = () => setIsOpen(s => !s);

    function goToClick(e: MouseEvent<HTMLAnchorElement>) {
        e.preventDefault();
        router.push(e.currentTarget.href);
    }

    return (
        <DropdownMenu
            isOpen={isOpen} size={'s'}
            onRequestClose={() => setIsOpen(false)}
            minWidth={'trigger'}
            trigger={
                <Button
                    size={'xs'} mode={'outline'}
                    iconPosition={'right'} icon={<DownIcon className={cn(styles.icon, isOpen && styles.iconRotate)}/>}
                    onClick={toggleIsOpen}
                >
                    {current}
                </Button>
            }
        >
            {!isLoading && menu.map(item => (
                <MenuItem
                    key={item.href} {...item}
                    selected={pathname === item.href} onClick={goToClick}
                >{item.children}</MenuItem>
            ))}
        </DropdownMenu>
    );
}
