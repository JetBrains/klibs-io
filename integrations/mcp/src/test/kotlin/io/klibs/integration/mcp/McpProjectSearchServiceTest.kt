package io.klibs.integration.mcp

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.service.SearchService
import io.klibs.integration.mcp.service.McpProjectSearchService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class McpProjectSearchServiceTest {

    private val searchService = mock<SearchService>()
    private val packageService = mock<PackageService>()
    private val uut = McpProjectSearchService(searchService, packageService)

    @Test
    fun `searchProjects returns projects with packages`() {
        val projectResult = createSearchProjectResult(
            id = 1,
            name = "kstatemachine",
            ownerLogin = "KStateMachine",
            platforms = listOf(PackagePlatform.JVM, PackagePlatform.NATIVE)
        )

        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(listOf(projectResult))

        val packageOverview = createPackageOverview(
            groupId = "io.github.kstatemachine",
            artifactId = "kstatemachine-core",
            version = "0.32.0-alpha",
            latestStableVersion = "0.31.1",
            description = "KStateMachine core module"
        )
        whenever(packageService.getLatestPackagesByProjectId(1))
            .thenReturn(listOf(packageOverview))

        val result = uut.mcpProjectSearch(
            query = "state machine",
            platforms = emptyList(),
            targetFilters = emptyMap(),
        )

        assertEquals(1, result.projects.size)
        val project = result.projects[0]
        assertEquals("kstatemachine", project.project.name)
        assertEquals("KStateMachine", project.project.ownerLogin)
        assertEquals(listOf(PackagePlatform.JVM, PackagePlatform.NATIVE), project.project.platforms)
        assertEquals(1, project.packages.size)
        assertEquals(1, project.totalPackages)
        assertEquals("io.github.kstatemachine", project.packages[0].groupId)
        assertEquals("kstatemachine-core", project.packages[0].artifactId)
        assertEquals("0.32.0-alpha", project.packages[0].version)
        assertEquals("0.31.1", project.packages[0].latestStableVersion)
        assertEquals("KStateMachine core module", project.packages[0].description)
    }

    @Test
    fun `searchProjects returns empty list when no projects found`() {
        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(emptyList())

        val result = uut.mcpProjectSearch(
            query = "nonexistent",
            platforms = emptyList(),
            targetFilters = emptyMap(),
        )

        assertTrue(result.projects.isEmpty())
    }

    @Test
    fun `searchProjects caps packages at the default limit and reports totalPackages`() {
        val projectResult = createSearchProjectResult(id = 1, name = "big-project", ownerLogin = "owner")

        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(listOf(projectResult))

        val packages = (1..25).map { i ->
            createPackageOverview("group", "artifact-$i", "1.0.0")
        }
        whenever(packageService.getLatestPackagesByProjectId(1)).thenReturn(packages)

        val result = uut.mcpProjectSearch(
            query = "big",
            platforms = emptyList(),
            targetFilters = emptyMap(),
        )

        assertEquals(1, result.projects.size)
        assertEquals(10, result.projects[0].packages.size)
        assertEquals(25, result.projects[0].totalPackages)
    }

    @Test
    fun `searchProjects honours an explicit maxPackagesPerProject`() {
        val projectResult = createSearchProjectResult(id = 1, name = "big-project", ownerLogin = "owner")

        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(listOf(projectResult))

        val packages = (1..25).map { i ->
            createPackageOverview("group", "artifact-$i", "1.0.0")
        }
        whenever(packageService.getLatestPackagesByProjectId(1)).thenReturn(packages)

        val result = uut.mcpProjectSearch(
            query = "big",
            platforms = emptyList(),
            targetFilters = emptyMap(),
            maxPackagesPerProject = 3,
        )

        assertEquals(3, result.projects[0].packages.size)
        assertEquals(25, result.projects[0].totalPackages)
    }

    @Test
    fun `searchProjects orders packages by dependent count then newest first`() {
        val projectResult = createSearchProjectResult(id = 1, name = "proj", ownerLogin = "owner")

        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(listOf(projectResult))

        // popular is older but most-depended; newerNiche is newest but unused; oldNiche ties on 0 dependents
        val popular = createPackageOverview("group", "popular", "1.0.0", dependentCount = 42, releasedAt = Instant.parse("2020-01-01T00:00:00Z"))
        val newerNiche = createPackageOverview("group", "newer-niche", "2.0.0", dependentCount = 0, releasedAt = Instant.parse("2024-01-01T00:00:00Z"))
        val oldNiche = createPackageOverview("group", "old-niche", "1.0.0", dependentCount = 0, releasedAt = Instant.parse("2019-01-01T00:00:00Z"))
        whenever(packageService.getLatestPackagesByProjectId(1)).thenReturn(listOf(oldNiche, newerNiche, popular))

        val result = uut.mcpProjectSearch(
            query = "proj",
            platforms = emptyList(),
            targetFilters = emptyMap(),
        )

        // most dependents first; ties broken by newest release
        assertEquals(listOf("popular", "newer-niche", "old-niche"), result.projects[0].packages.map { it.artifactId })
    }

    private fun createSearchProjectResult(
        id: Int = 1,
        name: String = "test-project",
        ownerLogin: String = "test-owner",
        platforms: List<PackagePlatform> = listOf(PackagePlatform.COMMON)
    ) = SearchProjectResult(
        id = id,
        name = name,
        repoName = name,
        description = "Test project",
        vcsStars = 100,
        ownerType = ScmOwnerType.ORGANIZATION,
        ownerLogin = ownerLogin,
        licenseName = "Apache-2.0",
        latestVersion = "1.0.0",
        latestVersionPublishedAt = Instant.now(),
        platforms = platforms,
        targets = emptyList(),
        tags = emptyList(),
        markers = emptyList()
    )

    private fun createPackageOverview(
        groupId: String,
        artifactId: String,
        version: String,
        latestStableVersion: String? = version,
        description: String? = "Test package",
        dependentCount: Int = 0,
        releasedAt: Instant = Instant.now()
    ) = PackageOverview(
        id = 1L,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        latestStableVersion = latestStableVersion,
        releasedAt = releasedAt,
        description = description,
        dependentCount = dependentCount,
        targets = listOf(PackageTarget(PackagePlatform.COMMON, null))
    )
}
