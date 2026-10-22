import type { ResolvingMetadata } from "next";
import { ProjectReference } from "@/app/types";

export function toCategorySlug(categoryName: string): string {
    return categoryName.toLowerCase().replace(/\s+/g, '-');
}

export async function socialMetadata(parent: ResolvingMetadata, title: string, description: string) {
    const resolved = await parent;
    return {
        openGraph: { title, description, images: resolved.openGraph?.images },
        twitter: { card: 'summary_large_image' as const, title, description, images: resolved.twitter?.images },
    };
}

export function getCompareLink(projects: ProjectReference[]): string {
    const params = new URLSearchParams({projects: projects.map(it => `${it.ownerLogin}/${it.projectName}`).join(',')});
    return `/compare?${params}`;
}

// Parses `projects=owner/name,owner/name` search params, skipping malformed and duplicate values
export function parseCompareParams(value: string | string[] | undefined): ProjectReference[] {
    const values = new Set([value ?? []].flat().flatMap(it => it.split(',')));
    return Array.from(values).flatMap(it => {
        const [ownerLogin, projectName, ...rest] = it.split('/');
        return ownerLogin && projectName && rest.length === 0 ? [{ownerLogin, projectName}] : [];
    });
}
