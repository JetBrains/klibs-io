import {ReactNode} from "react";
import cn from "classnames";
import Link from "next/link";

import {textCn} from "@rescui/typography";

import {getOwnerLink, PackageDetails, PackageOverview, ProjectDetails} from "@/app/types";
import {BreadcrumbsSelector, BreadcrumbsSelectorProps} from "@/app/ui/breadcrumb/selector";
import {usePathname} from "next/navigation";

import styles from './styles.module.css';

export function ProjectBreadcrumb({projectOverview}: { projectOverview: ProjectDetails }) {
    return <Breadcrumbs menu={[
        {href: getOwnerLink(projectOverview), children: projectOverview.ownerLogin},
        {children: projectOverview.name},
    ]}/>;
}

export function CategoryBreadcrumb({categoryName}: { categoryName: string }) {
    return <Breadcrumbs menu={[
        {href: '/', children: 'Main'},
        {children: categoryName},
    ]}/>;
}

export function PackageBreadcrumbs({projectPackage, packageVersions, version, projectPackages, parentProject}: {
    version?: string,
    projectPackage: PackageDetails,
    projectPackages?: PackageOverview[] | null
    packageVersions: PackageOverview[] | null,
    parentProject?: ProjectDetails | null,
}) {
    const organizationLink = `/organization/${parentProject?.ownerLogin}/`;
    const parentLink = `/project/${parentProject?.ownerLogin}/${parentProject?.name}`;
    const artifactLink = `/package/${projectPackage.groupId}/${projectPackage.artifactId}`;

    const packages: BreadcrumbsSelectorProps = {
        current: `${projectPackage.groupId}:${projectPackage.artifactId}`,
    };

    if (projectPackages) {
        if (projectPackages.length < 1)
            packages.menu = [{href: artifactLink, children: projectPackage.artifactId}];
        else
            packages.menu = projectPackages.map(pkg => ({
                href: `/package/${pkg.groupId}/${pkg.artifactId}`,
                children: `${projectPackage.groupId}:${pkg.artifactId}`,
            }));
    }

    const menu: BreadcrumbsProps['menu'] = [
        {href: organizationLink, children: parentProject?.ownerLogin},
        {href: parentLink, children: parentProject?.name},
        packages,
    ]

    const versions = packageVersions && packageVersions.length ?
        packageVersions.map(settings => settings.version) :
        null;

    const list = versions?.sort().reverse();
    const current = version || list?.[0];

    if (current) {
        menu.push({
            current: current,
            menu: versions?.sort().reverse()
                .map(option => ({
                    href: artifactLink + '/' + option,
                    children: option,
                })),
        });
    }

    return <Breadcrumbs menu={menu} isPackagePage/>;
}

export type BreadcrumbsItem = Parameters<typeof Link>[0] | BreadcrumbsSelectorProps | { children: ReactNode };
export type BreadcrumbsProps = { menu: BreadcrumbsItem[], isPackagePage?: boolean };

function Item({pathname, item}: { pathname: string, item: BreadcrumbsItem }) {
    let isActive = false;
    let children: null | ReactNode;

    if ('current' in item && item.menu?.length === 1) {
        item = item.menu[0];
    }

    if ('href' in item && pathname === item.href) {
        item = {children: item.children};
    }

    if ('current' in item) {
        children = <BreadcrumbsSelector pathname={pathname} {...item}/>;
    } else if ('href' in item) {
        children = <Link {...item}/>;
    } else {
        isActive = true;
        children = item.children;
    }

    return (
        <li aria-current="page"
            className={cn("breadcrumb-item", {"active": isActive})}
        >{children}</li>
    );
}

export function Breadcrumbs({menu, isPackagePage}: BreadcrumbsProps) {
    const pathname = usePathname();
    return (
        <nav aria-label="breadcrumb" className={cn(textCn('rs-text-2'), {[styles.breadcrumbsContainer]: isPackagePage})}>
            <ol className="breadcrumb">
                {menu.map((item, i) => (
                    <Item key={i} pathname={pathname} item={item}/>
                ))}
            </ol>
        </nav>
    );
}
