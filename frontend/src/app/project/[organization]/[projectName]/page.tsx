import ProjectPageContent from './project-page-content'
import {getProjectDetails, getProjectPackages, getProjectReadme, NotFoundException} from "@/app/api";
import {notFound} from "next/navigation";
import {getPlatformName, sortedPlatforms} from "@/app/types";

type MetadataParamsProps = {
    organization: string;
    projectName: string;
};

export async function generateMetadata({ params }: { params: MetadataParamsProps }) {

    const projectDetails = await getProjectDetails(params?.organization, params?.projectName);

    if (projectDetails instanceof NotFoundException) {
        return notFound()
    }

    const displayName = projectDetails?.name || params?.projectName;
    const platforms = sortedPlatforms(projectDetails?.platforms ?? []).map(getPlatformName).join(', ');
    const title = platforms
        ? `${displayName} — KMP library for ${platforms}`
        : displayName;
    const summary = projectDetails?.description || '';
    const description = summary && platforms
        ? `Explore ${displayName} on Klibs.io: ${summary}. Supports ${platforms}. View packages, releases, repository links, license, and project activity.`
        : `Explore ${displayName} on Klibs.io. View packages, releases, repository links, license, and project activity.`;

    return {
        title,
        description,
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
    const projectDetails = await getProjectDetails(params.organization, params.projectName);

    if (projectDetails instanceof NotFoundException) {
        return notFound();
    }

    const [projectPackages, projectReadme] = await Promise.all([
        getProjectPackages(params.organization, params.projectName),
        getProjectReadme(params.organization, params.projectName),
    ]);

    return (
        <ProjectPageContent
            initialProject={projectDetails}
            initialPackages={projectPackages}
            initialReadme={projectReadme}
            projectName={params.projectName}
        />
    );
}
