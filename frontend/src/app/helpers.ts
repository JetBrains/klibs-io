import type { ResolvingMetadata } from "next";

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
