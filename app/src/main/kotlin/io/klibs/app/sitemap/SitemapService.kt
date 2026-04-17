package io.klibs.app.sitemap

import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.ProjectService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Service
class SitemapService(
    private val projectService: ProjectService,
    private val packageService: PackageService,
) {
    private val cachedSitemap = AtomicReference("")

    @Scheduled(fixedRate = 1, initialDelay = 0, timeUnit = TimeUnit.DAYS)
    fun cacheSitemap() {
        try {
            val sitemap = generateSitemap()
            cachedSitemap.set(sitemap)
        } catch (e: Exception) {
            logger.error("Unable to generate the sitemap", e)
        }
    }

    fun getSitemap(): String {
        return cachedSitemap.get()
    }

    fun generateSitemap(): String {
        logger.info("Generating sitemap")
        val projects = projectService.findAllForSitemap()
        val allPackages = packageService.findAllPackagesForSitemap()
        val today = LocalDate.now(ZoneOffset.UTC).toString()

        val result = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""")

            // Static pages
            appendUrl("/", today)
            appendUrl( "/faq", today)

            for (project in projects) {
                val path = "/project/${xmlEscape(project.ownerLogin)}/${xmlEscape(project.projectName)}"
                val lastmod = project.updatedAt.atZone(ZoneOffset.UTC).toLocalDate().toString()
                appendUrl(path, lastmod)
            }

            for (pkg in allPackages) {
                val path = "/package/${xmlEscape(pkg.groupId)}/${xmlEscape(pkg.artifactId)}"
                val lastmod = pkg.releaseTs.atZone(ZoneOffset.UTC).toLocalDate().toString()
                appendUrl(path, lastmod)
            }

            append("</urlset>")
        }

        logger.info("Finished generating sitemap, size: {}", result.length)
        return result
    }

    private fun StringBuilder.appendUrl(path: String, lastmod: String) {
        appendLine("  <url>")
        appendLine("    <loc>$WEBSITE_PREFIX$path</loc>")
        appendLine("    <lastmod>$lastmod</lastmod>")
        appendLine("  </url>")
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(SitemapService::class.java)

        private const val WEBSITE_PREFIX = "https://klibs.io"
    }
}
