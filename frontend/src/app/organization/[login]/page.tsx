import type {Metadata, ResolvingMetadata} from "next";
import {socialMetadata} from "@/app/helpers";
import OrganizationPageContent from './organization-page-content'
import {getOwnerDetails, searchProjects} from "@/app/api";
import {OwnerOrganization} from "@/app/types";

type MetadataParamsProps = {
    login: string;
};

export async function generateMetadata({params}: { params: Promise<MetadataParamsProps> }, parent: ResolvingMetadata): Promise<Metadata> {
    const {login} = await params;

    const org = await getOwnerDetails<OwnerOrganization>(login);

    const title = org?.name || login;
    const description = org?.description || '';

    return {
        title,
        description,
        ...await socialMetadata(parent, title, description),
    };
}

export default async function Page({params}: { params: Promise<MetadataParamsProps> }) {
    const {login} = await params;

    const [organization, projects] = await Promise.all([
        getOwnerDetails<OwnerOrganization>(login),
        searchProjects({owner: login, page: 1}),
    ]);

    return (
        <OrganizationPageContent
            initialOrganization={organization}
            initialProjects={projects}
        />
    );
}
