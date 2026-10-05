import {
    CategoryWithProjects,
    OwnerAuthor,
    OwnerOrganization,
    PackageDetails,
    PackageOverview,
    PackageSearchResults,
    ProjectDetails,
    ProjectSearchResults,
    SearchParams,
    TagsStats,
    TargetGroups,
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

export const packageSearchResult = (
    overrides: Partial<PackageSearchResults> = {},
): PackageSearchResults => ({
    id: 1,
    groupId: 'io.arrow-kt',
    artifactId: 'arrow-core',
    description: 'Functional companion to Kotlin standard library',
    scmLink: 'https://github.com/arrow-kt/arrow',
    ownerType: 'organization',
    ownerLogin: 'arrow-kt',
    licenseName: 'Apache-2.0',
    latestVersion: '2.0.0',
    releaseTsMillis: 1_700_000_000_000,
    targetGroups: { JVM: ['11', '17'] },
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

// Release history as the API returns it: newest first.
export const packageVersionHistory = (): [PackageOverview, PackageOverview] => [
    packageOverview({ id: 1, version: '1.10.2', releasedAtMillis: 1_700_000_000_000 }),
    packageOverview({ id: 2, version: '1.9.0-RC.2', releasedAtMillis: 1_600_000_000_000 }),
];

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

export const tagsStats = (): TagsStats => ({
    totalProjectsCount: 3200,
    tags: ['compose-ui', 'networking', 'serialization', 'coroutines', 'testing', 'database']
        .map((tag, i) => ({ tag, projectsCount: 400 - i * 50 })),
});

export const networkingProjects = (): ProjectSearchResults[] => [
    projectSearchResult({
        id: 1,
        name: 'Ktor',
        ownerLogin: 'ktorio',
        description: 'Framework for quickly creating connected applications in Kotlin',
        markers: ['FEATURED'],
        scmStars: 13_400,
        tags: ['networking', 'http-client', 'server'],
    }),
    projectSearchResult({
        id: 2,
        name: 'Apollo Kotlin',
        ownerLogin: 'apollographql',
        description: 'A strongly-typed, caching GraphQL client for the JVM, Android, and Kotlin Multiplatform',
        scmStars: 3_900,
        tags: ['graphql', 'networking'],
    }),
    projectSearchResult({
        id: 3,
        name: 'Ktorfit',
        ownerLogin: 'Foso',
        description: 'HTTP client generator for Kotlin Multiplatform based on Ktor',
        scmStars: 1_600,
        tags: ['networking', 'http-client'],
    }),
];

export const homeCategories = (): CategoryWithProjects[] => [
    { category: { name: 'Grant winners', markers: [] }, projects: [] },
    { category: { name: 'Networking', markers: [] }, projects: networkingProjects() },
];

export const serializationPackages = (): PackageSearchResults[] => [
    packageSearchResult({
        id: 1,
        artifactId: 'kotlinx-serialization-json',
        groupId: 'org.jetbrains.kotlinx',
        ownerLogin: 'Kotlin',
        description: 'Kotlin multiplatform serialization: JSON format',
        scmLink: 'https://github.com/Kotlin/kotlinx.serialization',
    }),
    packageSearchResult({
        id: 2,
        artifactId: 'kotlinx-serialization-cbor',
        groupId: 'org.jetbrains.kotlinx',
        ownerLogin: 'Kotlin',
        description: 'Kotlin multiplatform serialization: CBOR format',
        scmLink: 'https://github.com/Kotlin/kotlinx.serialization',
    }),
    packageSearchResult({
        id: 3,
        artifactId: 'kaml',
        groupId: 'com.charleskorn.kaml',
        ownerLogin: 'charleskorn',
        description: 'YAML support for kotlinx.serialization',
        scmLink: 'https://github.com/charleskorn/kaml',
    }),
];

export const authorProjects = (): ProjectSearchResults[] => [
    projectSearchResult({
        id: 1,
        name: 'kmp-date-picker',
        ownerLogin: 'alice',
        description: 'A Compose Multiplatform date and time picker',
        scmStars: 1_600,
        tags: ['compose-ui', 'date-time'],
    }),
    projectSearchResult({
        id: 2,
        name: 'kmp-result',
        ownerLogin: 'alice',
        description: 'A multiplatform Result type for modelling success or failure',
        markers: ['FEATURED'],
        tags: ['functional-programming', 'error-handling'],
    }),
    projectSearchResult({
        id: 3,
        name: 'kmp-settings',
        ownerLogin: 'alice',
        description: 'Save simple key-value data on every Kotlin Multiplatform target',
        scmStars: 1_900,
        tags: ['storage'],
    }),
];

// A bundled image, so a capture never depends on GitHub's avatar CDN.
export const bundledAvatarUrl = (): string => new URL('../app/img/kodee/kodee-surprised.png', import.meta.url).href;

export const organizationProjects = (): ProjectSearchResults[] => [
    projectSearchResult({
        id: 1,
        name: 'Ktor',
        ownerLogin: 'jetbrains',
        description: 'Framework for quickly creating connected applications in Kotlin',
        markers: ['GRANT_WINNER_2024'],
        scmStars: 13_400,
        tags: ['networking', 'server'],
    }),
    projectSearchResult({
        id: 2,
        name: 'Exposed',
        ownerLogin: 'jetbrains',
        description: 'Kotlin SQL framework',
        markers: ['FEATURED'],
        scmStars: 8_700,
        tags: ['database', 'orm'],
    }),
    projectSearchResult({
        id: 3,
        name: 'kotlinx.coroutines',
        ownerLogin: 'jetbrains',
        description: 'Library support for Kotlin coroutines with multiplatform support',
        scmStars: 13_100,
        targetGroups: { JVM: ['11'], IOS: ['ios_arm64'], JavaScript: ['ir'], WasmJs: ['wasm_js'] },
        tags: ['coroutines', 'concurrency'],
    }),
];

export const insightsLeaderboard = (): Pick<ProjectDetails, 'id' | 'name' | 'ownerLogin' | 'scmStars' | 'dependentCount'>[] => [
    { id: 1, name: 'kotlinx.coroutines', ownerLogin: 'Kotlin', scmStars: 13_100, dependentCount: 9_800 },
    { id: 2, name: 'ktor', ownerLogin: 'ktorio', scmStars: 13_400, dependentCount: 4_200 },
    { id: 3, name: 'arrow', ownerLogin: 'arrow-kt', scmStars: 6_300, dependentCount: 1_900 },
];

export const multiplatformTargets = (): TargetGroups => ({
    JVM: ['11', '17'],
    IOS: ['ios_arm64', 'ios_x64'],
    JavaScript: ['ir'],
    AndroidJvm: ['android'],
});

// Newest first, as the API returns them.
export const packageReleases = (): PackageOverview[] => [
    packageOverview({
        id: 3, version: '2.0.0', releasedAtMillis: Date.UTC(2025, 0, 10), targetGroups: multiplatformTargets(),
    }),
    packageOverview({
        id: 2, version: '1.2.4', releasedAtMillis: Date.UTC(2024, 5, 2), targetGroups: multiplatformTargets(),
    }),
    packageOverview({
        id: 1, version: '1.1.5', releasedAtMillis: Date.UTC(2023, 8, 21), targetGroups: { JVM: ['11'] },
    }),
];

const arrowFxDescription = 'Structured concurrency and resource safety for Arrow';

export const groupArtifacts = (): PackageOverview[] => [
    packageOverview(),
    packageOverview({ id: 4, artifactId: 'arrow-fx-coroutines', description: arrowFxDescription }),
];

export const featuredProjectDetails = (overrides: Partial<ProjectDetails> = {}): ProjectDetails => projectDetails({
    markers: ['FEATURED'],
    targetGroups: { JVM: ['11', '17'], IOS: ['ios_arm64'], JavaScript: ['ir'] },
    tags: ['functional-programming', 'typed-errors'],
    ...overrides,
});

export const projectPackages = (): PackageOverview[] => [
    packageOverview(),
    packageOverview({
        id: 2, artifactId: 'arrow-fx-coroutines', description: arrowFxDescription, targetGroups: { JVM: ['11'] },
    }),
];

export const projectReadme = (): string =>
    '<h1>Arrow</h1><p>Arrow is a library for <strong>typed functional programming</strong> in Kotlin.</p>'
    + '<pre><code>implementation("io.arrow-kt:arrow-core:2.0.0")</code></pre>';

export const cardTargetGroups = (): TargetGroups => ({
    AndroidJvm: [],
    JVM: ['11', '17'],
    IOS: [],
    JavaScript: [],
});

export const allPlatformTargets = (): TargetGroups => ({
    AndroidJvm: ['11'],
    JVM: ['11', '17'],
    IOS: ['iosArm64', 'iosSimulatorArm64', 'iosX64'],
    MacOS: ['macosArm64', 'macosX64'],
    Wasm: ['wasmJs'],
    JavaScript: ['js'],
});

export const platformFilters = (): SearchParams => ({
    mode: 'projects',
    page: 1,
    platforms: ['ios', 'jvm'],
    query: '',
    tags: [],
});
