import styles from './styles.module.css';
import React from "react";
import cn from "classnames";

interface ContainerProps {
    children?: React.ReactNode;
    mode?: string;
    row?: boolean;
    nav?: boolean;
    smallColumn?: boolean;
    split?: boolean;
    cardGrid?: boolean;
    cardColumn?: boolean;
    className?: string;
    dataTestId?: string;
}

const Container: React.FC<ContainerProps> = (
    {
        children,
        mode,
        row,
        nav,
        smallColumn,
        split,
        cardGrid,
        cardColumn,
        className,
        dataTestId
    }) =>{
    return (
        <div
            className={cn(
                mode && styles[mode],
                !mode && styles.container,
                row && styles.row,
                nav && styles.nav,
                smallColumn && styles.smallColumn,
                split && styles.split,
                cardGrid && styles.cardGrid,
                cardColumn && styles.cardColumn,
                className && className
            )}
            data-testid={dataTestId}
        >
        {children}
        </div>
    );
}

export default Container;
