import AuthorPageContent from './author-page-content'
import {getOwnerDetails, searchProjects} from "@/app/api";
import {OwnerAuthor} from "@/app/types";

type MetadataParamsProps = {
    login: string;
};

export async function generateMetadata({params}: { params: MetadataParamsProps }) {
    const author = await getOwnerDetails<OwnerAuthor>(params.login);

    const title = author?.name || params.login;
    const description = author?.description || '';

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

export default async function Page({params}: { params: MetadataParamsProps }) {
    const [author, projects] = await Promise.all([
        getOwnerDetails<OwnerAuthor>(params.login),
        searchProjects({owner: params.login, page: 1}),
    ]);

    return (
        <AuthorPageContent
            initialAuthor={author}
            initialProjects={projects}
        />
    );
}
