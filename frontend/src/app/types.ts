export interface ProjectSearchResults {
	id: number;
	name: string;
	description: null | string;
	scmLink: string;
	scmStars: number;
	ownerType: 'author' | 'organization',
	ownerLogin: string;
	licenseName: string;
	latestReleaseVersion: string;
	latestReleasePublishedAtMillis: number;
	platforms: Platform[];
	tags: string[];
	markers: string[];
}

export interface ProjectDetails extends ProjectSearchResults {
	latestVersion: string;
	latestVersionPublicationDate: string;
	createdAtMillis: number;
	openIssues: null | number;
	linkIssues: null | string;
	lastActivityAtMillis: number;
	linkHomepage: string;
	linkScm: string;
	linkGitHubPages: string;
	linkWiki?: string;
	updatedAtMillis: number;
}

export interface PackageSearchResults {
	id: number;
	groupId: string;
	artifactId: string;
	description: string | null;
	scmLink: string;
	ownerType: 'organization' | 'author';
	ownerLogin: string;
	licenseName: string;
	latestVersion: string;
	releaseTsMillis: number;
	platforms: Platform[];
	targets: string[];
}

export function getProjectLink(projectOverview: ProjectSearchResults) {
	return `/project/${projectOverview.ownerLogin}/${projectOverview.name}`;
}

export function getOwnerPrefix(projectOverview: ProjectSearchResults) {
	return projectOverview.ownerType == 'author' ? 'author' : 'organization';
}

export function getOwnerLink(projectOverview: ProjectSearchResults) {
	return `/${getOwnerPrefix(projectOverview)}/${projectOverview.ownerLogin}`;
}

export function isFeaturedProject(projectOverview: ProjectSearchResults) {
	return projectOverview.markers && projectOverview.markers.includes('FEATURED');
}

export function isGrantWinner(projectOverview: ProjectSearchResults) {
	return projectOverview.markers && projectOverview.markers.some(marker => marker.startsWith('GRANT_WINNER'));

}

export enum Platform {
	common = 'common',
	jvm = 'jvm',
	androidJvm = 'androidJvm',
	native = 'native',
	wasm = 'wasm',
	js = 'js'
}

export const platformOrder = [
	Platform.androidJvm,
	Platform.jvm,
	Platform.native,
	Platform.wasm,
	Platform.js,
	Platform.common,
];

export const sortedPlatforms = (platforms: Platform[]) => {
    const filteredCommonPlatform = platforms.filter((platform) => platform !== Platform.common);
	return filteredCommonPlatform.sort((a, b) => {
		return platformOrder.indexOf(a) - platformOrder.indexOf(b);
	});
}

type Target = string;// to many variants, i skip it for now

interface PackageTarget {
	target: Target;
	platform: Platform;
}

export interface PackageOverview {
	id: number;
	groupId: string;
	artifactId: string;
	version: string;
	releasedAtMillis: number;
	targets: PackageTarget[];
	description: null | string;
}

interface Developer {
	title: string;
	url: string;
}

interface License {
	title: string;
	url: string;
}

export interface PackageDetails extends PackageOverview {
	projectId: null | number;
	name: null | string;
	licenses: License[];
	developers: Developer[];
	buildTool: string;
	kotlinVersion: string;
	linkHomepage: null | string;
	linkScm: string;
	linkFiles: null | string;
}


export function getPackageCoordinates(packageOverview: PackageOverview) {
	return `${packageOverview.groupId}:${packageOverview.artifactId}:${packageOverview.version}`;
}

export function getUniquePlatforms(packageOverview: PackageOverview): Platform[] {
	const platforms = packageOverview.targets.map(target => target.platform);
	const uniquePlatforms = Array.from(new Set(platforms));

	return sortedPlatforms(uniquePlatforms);
}

export function toTargetGroups(targets: PackageTarget[], native: boolean = false) {
	const filteredTargets = (native ? targets.filter(target => target.platform === Platform.native) : targets).filter(target => target.platform !== Platform.common);
	const grouped = filteredTargets.reduce((acc, curr) => {
		const platformName = getPlatformName(curr.platform);
		const target = curr.target;
		let existingPlatform = acc.find((item) => item.platformName === platformName);
		let groupId = "";
		const targetId = target;

		if (curr.platform === Platform.native && target.includes('_')) {
			const targetPrefix = targetId.split(/_(.*)/)[0];
			groupId = mapNativeTargetToGroupName(targetPrefix);
		}

		if (!existingPlatform) {
			existingPlatform = {
				platformId: curr.platform,
				platformName,
				groups: []
			}

			acc.push(existingPlatform);
		}

		let existingGroup = existingPlatform.groups.find(group => group.groupId === groupId);

		if (!existingGroup) {
			existingGroup = {
				groupId: groupId,
				targets: [],
			}

			existingPlatform.groups.push(existingGroup);
		}

		existingGroup.targets.push(targetId);

		return acc;
	}, [] as { platformId: Platform; platformName: string; groups: {groupId: string, targets: string[]}[] }[]);

	return grouped.sort((a, b) => {
		return platformOrder.indexOf(a.platformId as Platform) - platformOrder.indexOf(b.platformId as Platform);
	});
}


export function mapNativeTargetToGroupName(prefix: string) {
	switch (prefix) {
		case 'android':
			return 'Android Native';
		case 'ios':
			return 'iOS';
		case 'linux':
			return 'Linux';
		case 'macos':
			return 'macOS';
		case 'mingw':
			return 'Windows';
		case 'tvos':
			return 'tvOS';
		case 'watchos':
			return 'watchOS';
		case 'wasm':
			return 'Wasm';
		default:
			return 'Other';
	}
}

export function hasAnyLink(projectOverview: ProjectDetails): boolean {
	return !!projectOverview.linkHomepage ||
		!!projectOverview.linkScm ||
		!!projectOverview.linkGitHubPages ||
		!!projectOverview.linkIssues ||
		!!projectOverview.linkWiki;
}



export const getPlatformName = (platformId: Platform) => {
	if (platformId == Platform.androidJvm) {
		return 'Android JVM';
	} else if (platformId == Platform.common) {
		return 'Common';
	} else if (platformId == Platform.js) {
		return 'JS';
	} else if (platformId == Platform.jvm) {
		return 'JVM';
	} else if (platformId == Platform.native) {
		return 'Kotlin/Native';
	} else if (platformId == Platform.wasm) {
		return 'Wasm';
	} else {
		return 'Other';
	}
}

type OwnerType = "author" | "organization";

export interface Owner {
	type: OwnerType;
	id: number;
	login: string;
	avatarUrl: string;
	name: string;
	description: null | string;
	homepage: string;
	twitterHandle: string;
	email: string | null;
}

export interface OwnerAuthor extends Owner {
	"type": "author";
	location: string;
	followers: number;
	company: string;
}

export interface OwnerOrganization extends Owner{
	"type": "organization";
}

export type SearchSort = 'most-stars' | 'relevance';

export type SearchMode = 'projects' | 'packages';

export interface SearchParams {
	query?: string;
	platforms?: Platform[];
	sort?: SearchSort;
	page: number;
	limit?: number;
	owner?: string;
	tags?: string[];
	mode?: SearchMode;
	markers?: string[];
}

export interface TagsStats {
	totalProjectsCount: number;
	tags: TagsStatsItem[];
}

export interface TagsStatsItem {
	tag: string;
	projectsCount: number;
}

export interface Category {
	name: string;
	markers: string[];
}

export interface CategoriesResponse {
	categories: CategoryWithProjects[];
}

export interface CategoryWithProjects {
	category: Category;
	projects: ProjectSearchResults[];
}

//Converts numbers greater than 1 thousand to 0.0k format
export const kFormatter = (num: number) : string => {
	if (num > 999) {
		return (Math.abs(num) / 1000).toFixed(1) + 'k';
	} else {
		return "" + num;
	}
}
