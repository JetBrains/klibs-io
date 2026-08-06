package io.klibs.app.search

import BaseOpenSearchTest
import io.klibs.core.search.opensearch.IndexNaming
import io.klibs.core.search.opensearch.IndexSpec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SEED = "classpath:sql/OpenSearchIndexerTest/seed.sql"

@TestPropertySource(properties = [
    "klibs.search.opensearch.project-index=project-sync",
    "klibs.search.opensearch.package-index=package-sync",
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SearchIndexSyncTest : BaseOpenSearchTest() {

    @Autowired
    private lateinit var searchIndexSync: SearchIndexSync

    @Autowired
    private lateinit var naming: IndexNaming

    @Autowired
    private lateinit var client: OpenSearchClient

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun wipeSearchIndices() {
        runCatching {
            val indices = (
                runCatching { client.indices().get { it.index("project*", "package*") }.result().keys }.getOrDefault(emptySet()) +
                    runCatching { client.indices().getAlias { it.name("project*", "package*") }.result().keys }.getOrDefault(emptySet())
                )
                .filter { it.isSearchIndex() }
                .toSet()
                .toList()
            if (indices.isNotEmpty()) {
                client.indices().delete { it.index(indices) }
            }
        }
        jdbcClient.sql(
            """
                DELETE FROM shedlock
                WHERE name LIKE :projectLock
                   OR name LIKE :packageLock
            """.trimIndent()
        )
            .param("projectLock", "searchIndexSync-${naming.project.base}-%")
            .param("packageLock", "searchIndexSync-${naming.packages.base}-%")
            .update()
    }

    @Test
    @Sql(value = [SEED])
    fun `the boot build publishes both aliases on an empty cluster`() {
        searchIndexSync.buildMissingAliases()

        assertTrue(targetsOf(naming.project).isNotEmpty())
        assertTrue(targetsOf(naming.packages).isNotEmpty())
    }

    @Test
    @Sql(value = [SEED])
    fun `the boot build leaves an already published generation in place`() {
        searchIndexSync.buildMissingAliases()
        val serving = targetsOf(naming.project)

        searchIndexSync.buildMissingAliases()

        assertEquals(serving, targetsOf(naming.project))
    }

    private fun targetsOf(spec: IndexSpec): Set<String> =
        if (!client.indices().existsAlias { it.name(spec.alias) }.value()) emptySet()
        else client.indices().getAlias { it.name(spec.alias) }.result().keys

    private fun String.isSearchIndex(): Boolean =
        this == naming.project.base ||
            this == naming.packages.base ||
            startsWith("${naming.project.base}-") ||
            startsWith("${naming.packages.base}-")
}
