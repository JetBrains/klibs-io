export function toCategorySlug(categoryName: string): string {
    return categoryName.toLowerCase().replace(/\s+/g, '-');
}
