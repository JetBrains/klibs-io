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
	targetGroups: TargetGroups;
	tags: string[];
	markers: string[];
}

export interface ProjectDetails extends ProjectSearchResults {
	latestVersion: string;
	latestVersionPublicationDate: string;
	createdAtMillis: number;
	openIssues: null | number;
	linkIssues: null | string;
	dependentCount: number;
	lastActivityAtMillis: number;
	linkHomepage: string;
	linkScm: string;
	linkGitHubPages: string;
	linkWiki?: string;
	archived: boolean;
	archivedAtMillis: null | number;
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
	targets: TargetGroups;
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

export interface PackageOverview {
	id: number;
	groupId: string;
	artifactId: string;
	version: string;
	releasedAtMillis: number;
	targetGroups: TargetGroups;
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

export function hasTargetGroups(packageOverview: PackageOverview) {
	return !!packageOverview.targetGroups && Object.keys(packageOverview.targetGroups).length > 0;
}

export interface TargetGroupsByPlatform {
	platformId: Platform;
	platformName: string;
	groups: { groupId: string, targets: string[] }[];
}

// Groups the target groups by the platform they belong to, for the targets table.
export function toTargetGroupsByPlatform(targetGroups: TargetGroups, native: boolean = false): TargetGroupsByPlatform[] {
	const grouped = getSortedTargetGroups(targetGroups).reduce((acc, group) => {
		const platformId = getTargetGroupPlatform(group);

		if (platformId === Platform.common || (native && platformId !== Platform.native)) return acc;

		let platform = acc.find(item => item.platformId === platformId);

		if (!platform) {
			platform = {platformId, platformName: getPlatformName(platformId), groups: []};
			acc.push(platform);
		}

		platform.groups.push({groupId: getTargetGroupName(group), targets: targetGroups[group]});

		return acc;
	}, [] as TargetGroupsByPlatform[]);

	return grouped.sort((a, b) => platformOrder.indexOf(a.platformId) - platformOrder.indexOf(b.platformId));
}

// Backend TargetGroup name -> its targets (JVM versions for JVM/AndroidJvm, target names otherwise).
export type TargetGroups = Record<string, string[]>;

// Display order and labels for the backend TargetGroup names.
const targetGroupNames: Record<string, string> = {
	IOS: 'iOS',
	AndroidJvm: 'Android',
	AndroidNative: 'Android Native',
	JVM: 'JVM',
	MacOS: 'macOS',
	WatchOS: 'watchOS',
	TvOS: 'tvOS',
	Linux: 'Linux',
	Windows: 'Windows',
	Wasm: 'Wasm',
	JavaScript: 'JS',
	Unknown: 'Other',
};

const targetGroupOrder = Object.keys(targetGroupNames);

// Groups unknown to the map keep their raw name and go last.
const targetGroupIndex = (group: string) => {
	const index = targetGroupOrder.indexOf(group);
	return index === -1 ? targetGroupOrder.length : index;
};

// Platform each group belongs to, used for the badge colors.
const targetGroupPlatforms: Record<string, Platform> = {
	AndroidJvm: Platform.androidJvm,
	JVM: Platform.jvm,
	AndroidNative: Platform.native,
	IOS: Platform.native,
	MacOS: Platform.native,
	WatchOS: Platform.native,
	TvOS: Platform.native,
	Linux: Platform.native,
	Windows: Platform.native,
	Wasm: Platform.wasm,
	JavaScript: Platform.js,
	Unknown: Platform.common,
};

export const getTargetGroupName = (group: string) => targetGroupNames[group] || group;

export const getTargetGroupPlatform = (group: string) => targetGroupPlatforms[group] || Platform.common;

export function getSortedTargetGroups(targetGroups: TargetGroups): string[] {
	return Object.keys(targetGroups).sort((a, b) => targetGroupIndex(a) - targetGroupIndex(b));
}

export function getTargetGroupNames(targetGroups: TargetGroups): string[] {
	return getSortedTargetGroups(targetGroups).map(getTargetGroupName);
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

export type SearchSort = 'most-stars' | 'relevance'| 'most-dependents';

export type SearchMode = 'projects' | 'packages';

export type TargetGroupFilter = 'ios' | 'android' | 'jvm' | 'js' | 'wasm' | 'other';

export const targetGroupFilters: TargetGroupFilter[] = ['ios', 'android', 'jvm', 'js', 'wasm', 'other'];

const targetGroupFilterNames: Record<TargetGroupFilter, string> = {
	ios: 'iOS',
	android: 'Android',
	jvm: 'JVM',
	js: 'JavaScript',
	wasm: 'Wasm',
	other: 'Other',
};

export const getTargetGroupFilterName = (filter: TargetGroupFilter) => targetGroupFilterNames[filter];

// Each entry maps backend TargetGroup names to target names, matching
// List<Map<TargetGroup, Set<String>>> on the backend.
export type TargetGroupFilters = Record<string, string[]>[];

// Backend TargetGroup enum names, see core/package/.../model/TargetGroups.kt
// `Unknown` is rejected by the backend validator, so it is not filterable.
const targetGroupsByFilter: Record<TargetGroupFilter, string[]> = {
	ios: ['IOS'],
	android: ['AndroidNative', 'AndroidJvm'],
	jvm: ['JVM'],
	js: ['JavaScript'],
	wasm: ['Wasm'],
	other: ['Linux', 'MacOS', 'Windows', 'TvOS', 'WatchOS'],
};

// Groups inside one entry are OR-ed, entries in the list are AND-ed.
// An empty target list means "any target in this group".
export function toTargetGroupFilters(filters: TargetGroupFilter[] = []): TargetGroupFilters {
	return filters
		.map(filter => targetGroupsByFilter[filter] ?? [])
		.filter(groups => groups.length > 0)
		.map(groups => Object.fromEntries(groups.map(group => [group, [] as string[]])));
}

export function parseTargetGroupFilters(values: string[]): TargetGroupFilter[] {
	const legacyAliases: Record<string, TargetGroupFilter> = {androidJvm: 'android'};
	const parsed = values
		.map(value => targetGroupFilters.find(filter => filter === value) ?? legacyAliases[value])
		.filter((filter): filter is TargetGroupFilter => !!filter);

	return Array.from(new Set(parsed));
}

export interface SearchParams {
	query?: string;
	platforms?: TargetGroupFilter[];
	sort?: SearchSort;
	page: number;
	limit?: number;
	owner?: string;
	tags?: string[];
	mode?: SearchMode;
	markers?: string[];
}

// The non-nullable fields below are non-nullable on the backend and have no effective
// defaults there: omitting one, or sending null, is rejected with a 400.
export interface SearchProjectsRequest {
	query?: string;
	owner?: string;
	sortBy: SearchSort;
	tags: string[];
	markers: string[];
	targetGroupFilters: TargetGroupFilters;
}

export interface SearchPackagesRequest {
	query?: string;
	owner?: string;
	sortBy: SearchSort;
	targetGroupFilters: TargetGroupFilters;
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
