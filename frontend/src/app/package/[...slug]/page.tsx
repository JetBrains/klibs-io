import type {Metadata, ResolvingMetadata} from "next";
import {socialMetadata} from "@/app/helpers";
import PackagePageContent from './package-page-content'

import {
    getPackageDetails,
    getPackageVersionDetails,
    getPackageVersions,
    getGroupArtifacts,
    getProjectById
} from '@/app/api';
import {notFound} from "next/navigation";

type MetadataParamsProps = {
    slug: string[];
};

export async function generateMetadata({ params }: { params: Promise<MetadataParamsProps> }, parent: ResolvingMetadata): Promise<Metadata> {
    const {slug} = await params;

    const pkg = await getPackageDetails(slug[0], slug[1]);

    const parentProject = pkg.projectId
        ? await getProjectById(pkg.projectId)
        : null;

    const packageName = `${pkg.groupId}:${pkg.artifactId}`;
    const projectName = parentProject?.name;
    const title = projectName
        ? `${packageName} — package for ${projectName} KMP library`
        : packageName;
    const description = projectName
        ? `Package from ${projectName} on Klibs.io. Check supported targets, versions, repository links, and release details.`
        : `Package ${packageName} on Klibs.io. Check supported targets, versions, repository links, and release details.`;

    const canonical = `/package/${pkg.groupId}/${pkg.artifactId}/`;

    return {
        title,
        description,
        alternates: {
            canonical,
        },
        ...await socialMetadata(parent, title, description),
    };
}

export default async function Page({ params }: { params: Promise<MetadataParamsProps> }) {
    const {slug} = await params;

    const groupId = slug[0];
    const artifactId = slug[1];
    const version = slug[2];

    try {
        const packageDetails = version
            ? await getPackageVersionDetails(groupId, artifactId, version)
            : await getPackageDetails(groupId, artifactId);

        const parentProject = packageDetails.projectId
            ? await getProjectById(packageDetails.projectId)
            : null;

        const [packageVersions, groupArtifacts] = await Promise.all([
            getPackageVersions(groupId, artifactId),
            getGroupArtifacts(groupId),
        ]);

        return (
            <PackagePageContent
                initialPackage={packageDetails}
                initialParentProject={parentProject}
                initialPackageVersions={packageVersions}
                initialGroupArtifacts={groupArtifacts}
                version={version}
            />
        );
    } catch {
        notFound();
    }
}
