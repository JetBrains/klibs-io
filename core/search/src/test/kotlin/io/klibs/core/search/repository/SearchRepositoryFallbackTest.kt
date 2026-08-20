package io.klibs.core.search.repository

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.search.configuration.properties.OpenSearchProperties
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.opensearch.metrics.SearchQueryMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class SearchRepositoryFallbackTest {

    private val registry = SimpleMeterRegistry()
    private val metrics = SearchQueryMetrics(registry, OpenSearchProperties())

    private val projectSpec = spec("project")
    private val packageSpec = spec("package")

    private val openSearchProjects = mock<ProjectSearchRepositoryOpenSearch>()
    private val postgresProjects = mock<ProjectSearchRepositoryJdbc>()
    private val projects = ProjectSearchRepositoryFallback(openSearchProjects, postgresProjects, projectSpec, metrics)

    private val openSearchPackages = mock<PackageSearchRepositoryOpenSearch>()
    private val postgresPackages = mock<PackageSearchRepositoryJdbc>()
    private val packages = PackageSearchRepositoryFallback(openSearchPackages, postgresPackages, packageSpec, metrics)

    @Test
    fun `project search returns OpenSearch results while OpenSearch is healthy`() {
        whenever(openSearchProjects.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()))
            .thenReturn(listOf(project(1)))

        assertEquals(listOf(project(1)), findProjects())
        assertNull(fallbacks("project"))
    }

    @Test
    fun `project search falls back to PostgreSQL when OpenSearch fails`() {
        whenever(openSearchProjects.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("connection refused"))
        whenever(postgresProjects.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()))
            .thenReturn(listOf(project(2)))

        assertEquals(listOf(project(2)), findProjects())
        assertEquals(1.0, fallbacks("project")?.count())
    }

    @Test
    fun `project search propagates sorts OpenSearch cannot serve instead of masking them`() {
        whenever(openSearchProjects.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any(), any(), any()))
            .thenThrow(UnsupportedOperationException("MOST_HEALTHY is not supported by OpenSearch"))

        assertThrows<UnsupportedOperationException> { findProjects(SearchSort.MOST_HEALTHY) }
        // A sort OpenSearch cannot serve is not degraded serving.
        assertNull(fallbacks("project"))
    }

    @Test
    fun `package search returns OpenSearch results while OpenSearch is healthy`() {
        whenever(openSearchPackages.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(listOf(pckg("a")))

        assertEquals(listOf(pckg("a")), findPackages())
    }

    @Test
    fun `package search falls back to PostgreSQL when OpenSearch fails`() {
        whenever(openSearchPackages.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenThrow(RuntimeException("connection refused"))
        whenever(postgresPackages.find(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(listOf(pckg("b")))

        assertEquals(listOf(pckg("b")), findPackages())
        assertEquals(1.0, fallbacks("package")?.count())
    }

    private fun fallbacks(index: String) = registry.find("klibs.search.fallback").tag("index", index).counter()

    private fun spec(base: String) = OpenSearchIndexSpec(
        base = base,
        settings = "{}",
        mappings = "{}",
        sql = "select 1",
    ) { it.toString() }

    private fun findProjects(sortBy: SearchSort = SearchSort.RELEVANCY) = projects.find(
        query = "ktor",
        platforms = emptyList(),
        targetFilters = emptyList(),
        ownerLogin = null,
        sortBy = sortBy,
        tags = emptyList(),
        markers = emptyList(),
        page = 1,
        limit = 10,
    )

    private fun findPackages() = packages.find(
        query = "ktor",
        platforms = emptyList(),
        targetFilters = emptyList(),
        ownerLogin = null,
        sortBy = SearchSort.RELEVANCY,
        page = 1,
        limit = 10,
    )

    private fun project(id: Int) = SearchProjectResult(
        id = id,
        name = "project-$id",
        repoName = "repo-$id",
        description = null,
        vcsStars = 0,
        ownerType = ScmOwnerType.AUTHOR,
        ownerLogin = "owner",
        licenseName = null,
        latestVersion = "1.0.0",
        latestVersionPublishedAt = Instant.EPOCH,
        platforms = emptyList(),
        targets = emptyList(),
        tags = emptyList(),
        markers = emptyList(),
        dependentCount = 0,
        ossHealthScore = null,
    )

    private fun pckg(artifactId: String) = SearchPackageResult(
        groupId = "io.klibs",
        artifactId = artifactId,
        description = null,
        ownerType = ScmOwnerType.AUTHOR,
        ownerLogin = "owner",
        licenseName = null,
        latestVersion = "1.0.0",
        releaseTs = Instant.EPOCH,
        platforms = emptyList(),
        targetsList = emptyList(),
        targetsMap = emptyMap(),
    )
}
