package io.klibs.integration.mcp

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.readme.service.ReadmeService
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.service.SearchService
import io.klibs.integration.mcp.service.McpProjectSearchService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    private val readmeService = mock<ReadmeService>()
    private val uut = McpProjectSearchService(searchService, packageService, readmeService)

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
            version = "0.31.1",
            description = "KStateMachine core module"
        )
        whenever(packageService.getLatestPackagesByProjectId(1))
            .thenReturn(listOf(packageOverview))

        whenever(packageService.getLatestPackageDetails("io.github.kstatemachine", "kstatemachine-core"))
            .thenReturn(
                createPackageDetails(
                    groupId = "io.github.kstatemachine",
                    artifactId = "kstatemachine-core",
                    version = "0.31.1"
                )
            )

        whenever(readmeService.readReadmeMd(any())).thenReturn("# KStateMachine\nA state machine library")

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
        assertEquals("io.github.kstatemachine", project.packages[0].groupId)
        assertEquals("kstatemachine-core", project.packages[0].artifactId)
        assertEquals("KStateMachine core module", project.packages[0].description)
        assertEquals("# KStateMachine\nA state machine library", project.readme)
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
    fun `searchProjects limits packages to 200 per project`() {
        val projectResult = createSearchProjectResult(id = 1, name = "big-project", ownerLogin = "owner")

        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(listOf(projectResult))

        val packages = (1..201).map { i ->
            createPackageOverview("group", "artifact-$i", "1.0.0")
        }
        whenever(packageService.getLatestPackagesByProjectId(1)).thenReturn(packages)

        whenever(packageService.getLatestPackageDetails("group", "artifact-1"))
            .thenReturn(createPackageDetails("group", "artifact-1", "1.0.0"))

        whenever(readmeService.readReadmeMd(any())).thenReturn(null)

        val result = uut.mcpProjectSearch(
            query = "big",
            platforms = emptyList(),
            targetFilters = emptyMap(),
        )

        assertEquals(1, result.projects.size)
        assertEquals(200, result.projects[0].packages.size)
    }

    @Test
    fun `searchProjects returns readme content when available`() {
        val projectResult = createSearchProjectResult(
            id = 1,
            name = "documented-project",
            ownerLogin = "author"
        )

        whenever(
            searchService.search(
                anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenReturn(listOf(projectResult))

        whenever(packageService.getLatestPackagesByProjectId(1)).thenReturn(emptyList())

        val readmeContent = """
            |# Documented Project
            |
            |This is a well-documented project with a detailed README.
            |
            |## Features
            |- Feature 1
            |- Feature 2
        """.trimMargin()
        whenever(readmeService.readReadmeMd(any())).thenReturn(readmeContent)

        val result = uut.mcpProjectSearch(
            query = "documented",
            platforms = emptyList(),
            targetFilters = emptyMap(),
        )

        assertEquals(1, result.projects.size)
        assertEquals(readmeContent, result.projects[0].readme)
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
        description: String? = "Test package"
    ) = PackageOverview(
        id = 1L,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        releasedAt = Instant.now(),
        description = description,
        targets = listOf(PackageTarget(PackagePlatform.COMMON, null))
    )

    private fun createPackageDetails(
        groupId: String,
        artifactId: String,
        version: String,
    ) = PackageDetails(
        id = 1L,
        projectId = 1,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        releasedAt = Instant.now(),
        description = "Test package",
        targets = emptyList(),
        licenses = emptyList(),
        developers = emptyList(),
        buildTool = "Gradle",
        buildToolVersion = "8.0",
        kotlinVersion = "2.0.0",
        url = null,
        scmUrl = null
    )
}
