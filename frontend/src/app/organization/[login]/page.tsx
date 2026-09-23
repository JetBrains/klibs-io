import type {ResolvingMetadata} from "next";
import {socialMetadata} from "@/app/helpers";
import OrganizationPageContent from './organization-page-content'
import {getOwnerDetails, searchProjects} from "@/app/api";
import {OwnerOrganization} from "@/app/types";

type MetadataParamsProps = {
    login: string;
};

export async function generateMetadata({params}: { params: MetadataParamsProps }, parent: ResolvingMetadata) {
    const org = await getOwnerDetails<OwnerOrganization>(params.login);

    const title = org?.name || params.login;
    const description = org?.description || '';

    return {
        title,
        description,
        ...await socialMetadata(parent, title, description),
    };
}

export default async function Page({params}: { params: MetadataParamsProps }) {
    const [organization, projects] = await Promise.all([
        getOwnerDetails<OwnerOrganization>(params.login),
        searchProjects({owner: params.login, page: 1}),
    ]);

    return (
        <OrganizationPageContent
            initialOrganization={organization}
            initialProjects={projects}
        />
    );
}
