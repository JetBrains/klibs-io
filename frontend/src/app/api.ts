import {
    CategoriesResponse,
    PackageDetails,
    PackageOverview,
    PackageSearchResults,
    ProjectDetails,
    ProjectSearchResults,
    SearchParams,
    TagsStats
} from "@/app/types";

export const getProjectById = async(id: number) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/project/${id}/details`, {
        next: { revalidate: 600 }
    });

    return await res.json() as ProjectDetails;
}

export class NotFoundException {
    public res: Response;

    constructor(res: Response) {
        this.res = res;
    }
}

export type ProjectDetailsResp = ProjectDetails | NotFoundException;

export const getProjectDetails = async(ownerLogin: string, projectName: string): Promise<ProjectDetailsResp> => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/project/${ownerLogin}/${projectName}/details`, {
        next: { revalidate: 600 }
    });
    if (!res.ok) {
        if (res.status === 404) return new NotFoundException(res);
    }

    return await res.json() as ProjectDetails;
}

export const getProjectPackages = async(ownerLogin: string, projectName: string) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/project/${ownerLogin}/${projectName}/packages`, {
        next: { revalidate: 600 }
    });

    return await res.json() as PackageOverview[];
}

export const getProjectReadme = async(ownerLogin: string, projectName: string): Promise<string> => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/project/${ownerLogin}/${projectName}/readme/markdown`, {
        next: { revalidate: 86400 }
    });

    const text = await res.text();


    return text.toString();
}

export const getPackageDetails = async(groupId: string, artifactId: string) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/package/${groupId}/${artifactId}/details`, {
        next: { revalidate: 600 }
    });

    return await res.json() as PackageDetails;
}

export const getPackageVersionDetails = async(groupId: string, artifactId: string, version: string) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/package/${groupId}/${artifactId}/${version}/details`, {
        next: { revalidate: 600 }
    });

    return await res.json() as PackageDetails;
}

export const getPackageVersions = async(groupId: string, artifactId: string) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/package/${groupId}/${artifactId}/versions`, {
        next: { revalidate: 600 }
    });

    return await res.json() as PackageOverview[];
}

export const getGroupArtifacts = async(groupId: string) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/package/${groupId}/artifacts`, {
        next: { revalidate: 600 }
    });

    return await res.json() as PackageOverview[];
}

export const getOwnerDetails = async<T>(login: string) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/owner/${login}/details`, {
        next: { revalidate: 600 }
    });

    return await res.json() as T;
}

export const searchProjects = async(searchParams: SearchParams) => {
    if (!searchParams.page) {
        searchParams.page = 1;
    }

    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/search/projects?${toQueryParams(searchParams)}`, {
        next: { revalidate: 600 }
    });

    return await res.json() as ProjectSearchResults[];
}

export const searchPackages = async(searchParams: SearchParams) => {
    if (!searchParams.page) {
        searchParams.page = 1;
    }

    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/search/packages?${toQueryParams(searchParams)}`, {
        next: { revalidate: 600 }
    });

    return await res.json() as PackageSearchResults[];
}

export const getTagsStats = async ({ limit }: { limit?: number}) => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/tags/stats${limit ? `?limit=${limit}` : ''}`, {
        next: { revalidate: 600 }
    });

    if (res.status !== 200) {
        return null;
    }

    return await res.json() as TagsStats;
}

export const getProjectsCount = async (): Promise<string> => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/tags/stats?limit=0`, {
        next: { revalidate: 86400 }
    });
    const data = await res.json() as TagsStats;
    const rounded = Math.floor(data.totalProjectsCount / 100) * 100;
    return String(rounded);
};

export const getCategoriesWithProjects = async (): Promise<CategoriesResponse | null> => {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/categories/projects`, {
        next: { revalidate: 3600 }
    });

    if (res.status !== 200) {
        return null;
    }

    return await res.json() as CategoriesResponse;
};

export function toQueryParams(params: SearchParams): string {
    const urlSearchParams = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if (Array.isArray(value)) {
            value.forEach(val => {
                urlSearchParams.append(key, val);
            });
        } else if (value !== undefined && value !== null) {
            urlSearchParams.append(key, String(value));
        }
    });

    if (!urlSearchParams.has("page")) {
        urlSearchParams.append("page", "1");
    }

    return urlSearchParams.toString();
}
