import {
    OwnerAuthor,
    OwnerOrganization,
    PackageDetails,
    PackageOverview,
    ProjectDetails,
    ProjectSearchResults,
} from '@/app/types';

export const projectSearchResult = (
    overrides: Partial<ProjectSearchResults> = {},
): ProjectSearchResults => ({
    id: 1,
    name: 'Arrow',
    description: 'Functional companion to Kotlin standard library',
    scmLink: 'https://github.com/arrow-kt/arrow',
    scmStars: 6_000,
    ownerType: 'organization',
    ownerLogin: 'arrow-kt',
    licenseName: 'Apache-2.0',
    latestReleaseVersion: '2.0.0',
    latestReleasePublishedAtMillis: 1_700_000_000_000,
    targetGroups: { JVM: ['11', '17'] },
    tags: ['functional-programming'],
    markers: [],
    ...overrides,
});

export const projectDetails = (overrides: Partial<ProjectDetails> = {}): ProjectDetails => ({
    ...projectSearchResult(),
    latestVersion: '2.0.0',
    latestVersionPublicationDate: '2025-01-01',
    createdAtMillis: 1_500_000_000_000,
    openIssues: 10,
    linkIssues: 'https://github.com/arrow-kt/arrow/issues',
    dependentCount: 100,
    lastActivityAtMillis: 1_700_000_000_000,
    linkHomepage: 'https://arrow-kt.io/',
    linkScm: 'https://github.com/arrow-kt/arrow',
    linkGitHubPages: 'https://apidocs.arrow-kt.io/',
    linkWiki: 'https://github.com/arrow-kt/arrow/wiki',
    archived: false,
    archivedAtMillis: null,
    updatedAtMillis: 1_700_000_000_000,
    ...overrides,
});

export const packageOverview = (overrides: Partial<PackageOverview> = {}): PackageOverview => ({
    id: 1,
    groupId: 'io.arrow-kt',
    artifactId: 'arrow-core',
    version: '2.0.0',
    releasedAtMillis: 1_700_000_000_000,
    targetGroups: { JVM: ['11', '17'] },
    description: 'Arrow core package',
    ...overrides,
});

export const packageDetails = (overrides: Partial<PackageDetails> = {}): PackageDetails => ({
    ...packageOverview(),
    projectId: 1,
    name: 'Arrow Core',
    licenses: [{ title: 'Apache-2.0', url: 'https://www.apache.org/licenses/LICENSE-2.0' }],
    developers: [{ title: 'Arrow maintainers', url: 'https://arrow-kt.io/community/' }],
    buildTool: 'Gradle 8',
    kotlinVersion: '2.0.0',
    linkHomepage: 'https://arrow-kt.io/',
    linkScm: 'https://github.com/arrow-kt/arrow',
    linkFiles: 'https://repo1.maven.org/maven2/io/arrow-kt/arrow-core/',
    ...overrides,
});

export const author = (overrides: Partial<OwnerAuthor> = {}): OwnerAuthor => ({
    type: 'author',
    id: 1,
    login: 'alice',
    avatarUrl: 'https://avatars.githubusercontent.com/u/1',
    name: 'Alice Example',
    description: 'Kotlin library author',
    homepage: 'https://alice.example.com',
    twitterHandle: 'alice_kotlin',
    email: 'alice@example.com',
    location: 'Amsterdam',
    followers: 42,
    company: 'Example Ltd',
    ...overrides,
});

export const organization = (
    overrides: Partial<OwnerOrganization> = {},
): OwnerOrganization => ({
    type: 'organization',
    id: 2,
    login: 'jetbrains',
    avatarUrl: 'https://avatars.githubusercontent.com/u/2',
    name: 'JetBrains',
    description: 'Creators of Kotlin',
    homepage: 'https://www.jetbrains.com/opensource/',
    twitterHandle: 'jetbrains',
    email: 'github@jetbrains.com',
    ...overrides,
});
