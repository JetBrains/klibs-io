import {MenuItem} from "@rescui/menu";
import {textCn} from '@rescui/typography';
import cn from "classnames";

import styles from './styles.module.css'

import {kotlinEcosystemMenu} from "@/app/ui/kotlin-ecosystem-dropdown/nav-scheme";

// Mobile menu
export function KotlinEcosystemMobileMenu() {
    return (
        <div className={styles.ktEcosystemMobileWrapper}>
            {kotlinEcosystemMenu.map((item, index) => {
                if (item.url) {
                    return (
                        <MenuItem size={'l'} key={`${index}`} href={item.url}>
                            {item.title}
                        </MenuItem>
                    );
                } else if (item.items && item.items.length > 0) {
                    return (
                        <div key={index} className={styles.mobileListGroup}>
                            <h5 className={cn(styles.mobileSectionHeading, textCn('rs-text-3'))}>{item.title}</h5>
                            <ul className={styles.menuList}>
                                {item.items.map((subItem, subIndex) => (
                                    <MenuItem size={'l'} key={`${subIndex}`} href={subItem.url || '#'}>
                                        {subItem.title}
                                    </MenuItem>
                                ))}
                            </ul>
                        </div>
                    );
                }
                return null;
            })}
        </div>
    );
};
