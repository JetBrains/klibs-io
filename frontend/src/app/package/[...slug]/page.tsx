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

export async function generateMetadata({ params }: { params: MetadataParamsProps }) {

    const pkg = await getPackageDetails(params.slug[0], params.slug[1]);

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
        openGraph: {
            title,
            description,
        },
        twitter: {
            title,
            description,
        },
    };
}

export default async function Page({ params }: { params: MetadataParamsProps }) {
    const groupId = params.slug[0];
    const artifactId = params.slug[1];
    const version = params.slug[2];

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
    } catch (error) {
        notFound();
    }
}
