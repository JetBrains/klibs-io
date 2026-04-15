package io.klibs.app.sitemap

import io.klibs.core.pckg.dto.projection.SitemapPackageView
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.ProjectService
import io.klibs.core.project.repository.SitemapProjectEntry
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class SitemapServiceTest {

    private val projectService: ProjectService = mock()
    private val packageService: PackageService = mock()
    private val sitemapService = SitemapService(projectService, packageService)

    @Test
    fun `generateSitemap should include static pages`() {
        whenever(projectService.findAllForSitemap()).thenReturn(emptyList())
        whenever(packageService.findAllPackagesForSitemap()).thenReturn(emptyList())

        val sitemap = sitemapService.generateSitemap()

        assertTrue(sitemap.contains("<loc>https://klibs.io/</loc>"))
        assertTrue(sitemap.contains("<loc>https://klibs.io/faq</loc>"))
    }

    @Test
    fun `generateSitemap should include project entries`() {
        val projects = listOf(
            SitemapProjectEntry("owner1", "project-a", Instant.parse("2025-03-15T10:00:00Z")),
            SitemapProjectEntry("owner2", "project-b", Instant.parse("2025-06-20T18:30:00Z")),
        )
        whenever(projectService.findAllForSitemap()).thenReturn(projects)
        whenever(packageService.findAllPackagesForSitemap()).thenReturn(emptyList())

        val sitemap = sitemapService.generateSitemap()

        assertTrue(sitemap.contains("<loc>https://klibs.io/project/owner1/project-a</loc>"))
        assertTrue(sitemap.contains("<lastmod>2025-03-15</lastmod>"))
        assertTrue(sitemap.contains("<loc>https://klibs.io/project/owner2/project-b</loc>"))
        assertTrue(sitemap.contains("<lastmod>2025-06-20</lastmod>"))
    }

    @Test
    fun `generateSitemap should include package entries`() {
        val packages = listOf(
            sitemapPackage("com.example", "lib-core", Instant.parse("2025-01-10T12:00:00Z")),
        )
        whenever(projectService.findAllForSitemap()).thenReturn(emptyList())
        whenever(packageService.findAllPackagesForSitemap()).thenReturn(packages)

        val sitemap = sitemapService.generateSitemap()

        assertTrue(sitemap.contains("<loc>https://klibs.io/package/com.example/lib-core</loc>"))
        assertTrue(sitemap.contains("<lastmod>2025-01-10</lastmod>"))
    }

    @Test
    fun `generateSitemap should XML-escape special characters in project paths`() {
        val projects = listOf(
            SitemapProjectEntry("owner&co", "lib<1>", Instant.parse("2025-05-01T00:00:00Z")),
        )
        whenever(projectService.findAllForSitemap()).thenReturn(projects)
        whenever(packageService.findAllPackagesForSitemap()).thenReturn(emptyList())

        val sitemap = sitemapService.generateSitemap()

        assertTrue(sitemap.contains("<loc>https://klibs.io/project/owner&amp;co/lib&lt;1&gt;</loc>"))
    }

    @Test
    fun `generateSitemap should produce valid XML structure`() {
        whenever(projectService.findAllForSitemap()).thenReturn(emptyList())
        whenever(packageService.findAllPackagesForSitemap()).thenReturn(emptyList())

        val sitemap = sitemapService.generateSitemap()

        assertTrue(sitemap.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(sitemap.contains("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">"""))
        assertTrue(sitemap.endsWith("</urlset>"))
    }

    private fun sitemapPackage(groupId: String, artifactId: String, releaseTs: Instant): SitemapPackageView {
        return object : SitemapPackageView {
            override val groupId = groupId
            override val artifactId = artifactId
            override val releaseTs = releaseTs
        }
    }
}
