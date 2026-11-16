import type {Metadata, ResolvingMetadata} from "next";
import {socialMetadata} from "@/app/helpers";
import AuthorPageContent from './author-page-content'
import {getOwnerDetails, searchProjects} from "@/app/api";
import {OwnerAuthor} from "@/app/types";

type MetadataParamsProps = {
    login: string;
};

export async function generateMetadata({params}: { params: Promise<MetadataParamsProps> }, parent: ResolvingMetadata): Promise<Metadata> {
    const {login} = await params;

    const author = await getOwnerDetails<OwnerAuthor>(login);

    const title = author?.name || login;
    const description = author?.description || '';

    return {
        title,
        description,
        ...await socialMetadata(parent, title, description),
    };
}

export default async function Page({params}: { params: Promise<MetadataParamsProps> }) {
    const {login} = await params;

    const [author, projects] = await Promise.all([
        getOwnerDetails<OwnerAuthor>(login),
        searchProjects({owner: login, page: 1}),
    ]);

    return (
        <AuthorPageContent
            initialAuthor={author}
            initialProjects={projects}
        />
    );
}
