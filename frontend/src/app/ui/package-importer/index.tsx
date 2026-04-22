import React, {useRef, useState} from "react";
import cn from "classnames";

import {Button} from "@rescui/button";
import {Tooltip} from "@rescui/tooltip";
import {CopyIcon} from "@rescui/icons";
import {Tab, TabList, TabSeparator} from "@rescui/tab-list";

import {getPackageCoordinates, PackageDetails, PackageOverview} from "@/app/types";
import {useLocalStorage} from "@/app/hooks";

import styles from './styles.module.css';
import {trackEvent, GAEvent} from "@/app/analytics";

export const GRADLE_LANGUAGE_ID = 'gradle-lang';

export function useGradleLanguage() {
    return useLocalStorage<TabId>(GRADLE_LANGUAGE_ID, 'kotlin');
}

const TABS_ENTRIES = [
    ['kotlin', {title: 'Gradle Kotlin', Wrapper: KotlinCode} as const] as const,
    ['groovy', {title: 'Gradle Groovy', Wrapper: GradleCode} as const] as const,
] as const;

type TabId = typeof TABS_ENTRIES[number][0];
type Tab = typeof TABS_ENTRIES[number][1];

type Tabs = Record<TabId, Tab>;

export const TABS = Object.freeze(Object.fromEntries(TABS_ENTRIES) as Tabs);

export function getKotlinCode(projectPackage: PackageOverview) {
    return `implementation("${getPackageCoordinates(projectPackage)}")`;
}

export function getGroovyCode(projectPackage: PackageOverview) {
    return `implementation '${getPackageCoordinates(projectPackage)}'`;
}

function KotlinCode({projectPackage}: { projectPackage: PackageDetails }) {
    return <>
        implementation(<span className={styles.codeHighlight}>
            &quot;{getPackageCoordinates(projectPackage)}&quot;
        </span>)
    </>;
}

function GradleCode({projectPackage}: { projectPackage: PackageDetails }) {
    return <>
        implementation <span className={styles.codeHighlight}>
            &apos;{getPackageCoordinates(projectPackage)}&apos;
        </span>
    </>;
}

export function PackageImportCode({projectPackage}: { projectPackage: PackageDetails }) {
    const [activeTab, setActiveTab] = useGradleLanguage();
    const [checked, setChecked] = useState(false);
    const [timeoutId, setTimeoutId] = useState(0);
    const contentNode = useRef<HTMLSpanElement>(null);

    const copyImplementation = () => {
        const node = contentNode.current;
        if (!node) return;

        const copyText = node.innerText;

        clearTimeout(timeoutId);
        setChecked(true);

        navigator.clipboard.writeText(copyText).then(() => {
            setTimeoutId(window.setTimeout(() => {
                setChecked(false);
            }, 1200));
        });
    };

    return <div>
        <TabList className="rs-docs-offset-top-12" value={activeTab} onChange={setActiveTab} mode={"rock"}>
            {TABS_ENTRIES.map(([key, value]) => (
                <Tab key={key} value={key} onClick={() => {trackEvent(GAEvent.PACKAGE_IMPORT_TAB_CLICK, {eventCategory: value.title})}}>{value.title}</Tab>
            ))}
        </TabList>

        <TabSeparator className={styles.separator}/>

        <code className={cn(styles.codeWrapper, "rs-code rs-pre")}>
            <span ref={contentNode}>{TABS[activeTab].Wrapper({projectPackage})}</span>
            <Tooltip
                sparse={false}
                isVisible={checked}
                placement="left"
                content="Code copied">
                <div className={styles.copyButton}>
                    <Button
                        icon={<CopyIcon/>}
                        content={"icon"}
                        mode={"clear"}
                        onClick={() => {copyImplementation(); trackEvent(GAEvent.PACKAGE_COPY_SNIPPET, {})}}
                    />
                </div>
            </Tooltip>
        </code>
    </div>
}
