import {DropdownMenu} from "@rescui/dropdown-menu";
import React, { useState } from "react";
import {Button} from "@rescui/button";
import {CopyIcon, MoreIcon} from "@rescui/icons";
import {MenuItem} from "@rescui/menu";
import styles from "./styles.module.css";
import {PackageOverview} from "@/app/types";
import {getGroovyCode, getKotlinCode} from "@/app/ui/package-importer";
import {trackEvent, GAEvent} from "@/app/analytics";

interface PackageImporterDropdownProps {
    packageOverview: PackageOverview;
    isPackagePage?: boolean;
}

export function PackageImporterDropdown({packageOverview, isPackagePage}: PackageImporterDropdownProps) {
    const [isOpen, setIsOpen] = useState(false);

    const toggleIsOpen = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.preventDefault();
        e.stopPropagation();
        trackEvent(GAEvent.PACKAGE_CONTEXT_MENU_BUTTON_CLICK, {});
        setIsOpen(s => !s)
    };

    const copyToClipboard = (type: "kotlin" | "groovy") => {
        navigator.clipboard.writeText(
            type === "kotlin" ?
                getKotlinCode(packageOverview) :
                getGroovyCode(packageOverview)
        ).then(() => setIsOpen(false));
    }

    const detailsPageUrl = isPackagePage
        ? `/package/${packageOverview.groupId}/${packageOverview.artifactId}/${packageOverview.version}`
        : `/package/${packageOverview.groupId}/${packageOverview.artifactId}`;

    return (
        <div className={styles.dropdown}>
            <DropdownMenu
                isOpen={isOpen}
                onRequestClose={() => setIsOpen(false)}
                trigger={<Button mode={'clear'} onClick={toggleIsOpen} icon={<MoreIcon />} className={styles.button} />}
                minWidth={'220px'}
            >
                <MenuItem href={detailsPageUrl} onClick={() => {trackEvent(GAEvent.PACKAGE_CONTEXT_MENU_DETAILS_CLICK, {})}}>Details page</MenuItem>
                <MenuItem icon={<CopyIcon />} onClick={() => {copyToClipboard('kotlin'); trackEvent(GAEvent.PACKAGE_CONTEXT_MENU_KOTLIN_SNIPPET_CLICK, {})}}>Gradle Kotlin snippet</MenuItem>
                <MenuItem icon={<CopyIcon />} onClick={() => {copyToClipboard('groovy'); trackEvent(GAEvent.PACKAGE_CONTEXT_MENU_GROOVY_SNIPPET_CLICK, {})}}>Gradle Groovy snippet</MenuItem>
            </DropdownMenu>
        </div>
    );
}
