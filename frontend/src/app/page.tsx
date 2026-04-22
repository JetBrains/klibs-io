//Index page
import { getCategoriesWithProjects, getProjectsCount } from "@/app/api";
import PageContent from "@/app/page-content";
import type { Metadata } from "next";

export async function generateMetadata(): Promise<Metadata> {
    const projectsCount = await getProjectsCount();
    const title = `Kotlin Multiplatform Libraries (KMP) – Explore ${projectsCount}+ | Klibs.io`;
    const description = 'AI-powered search for Kotlin Multiplatform libraries (KMP). Discover 2200+ tools, frameworks, and community libraries for building cross-platform apps with Kotlin Multiplatform.';

    return {
        title: { absolute: title },
        description,
        openGraph: { title, description },
        twitter: { title, description },
    };
}

export default async function Index() {
    const [categoriesResponse, projectsCount] = await Promise.all([
        getCategoriesWithProjects(),
        getProjectsCount(),
    ]);
    const categoryWithProjects = categoriesResponse?.categories || [];
    const categories = categoryWithProjects.map(c => c.category);

    return <PageContent categories={categories} categoryWithProjects={categoryWithProjects} projectsCount={projectsCount} />;
}
