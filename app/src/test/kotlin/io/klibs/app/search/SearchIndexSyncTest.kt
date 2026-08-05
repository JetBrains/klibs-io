package io.klibs.app.search

import BaseOpenSearchTest
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.opensearch.SearchIndexSync
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    @Qualifier("projectIndexSpec")
    private lateinit var projectSpec: OpenSearchIndexSpec

    @Autowired
    @Qualifier("packageIndexSpec")
    private lateinit var packageSpec: OpenSearchIndexSpec

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
            .param("projectLock", "searchIndexSync-${projectSpec.base}-%")
            .param("packageLock", "searchIndexSync-${packageSpec.base}-%")
            .update()
    }

    @Test
    @Sql(value = [SEED])
    fun `the boot build publishes both aliases on an empty cluster`() {
        searchIndexSync.buildMissingAliases()

        assertTrue(targetsOf(projectSpec).isNotEmpty())
        assertTrue(targetsOf(packageSpec).isNotEmpty())
    }

    @Test
    @Sql(value = [SEED])
    fun `the boot build leaves an already published generation in place`() {
        searchIndexSync.buildMissingAliases()
        val serving = targetsOf(projectSpec)

        searchIndexSync.buildMissingAliases()

        assertEquals(serving, targetsOf(projectSpec))
    }

    private fun targetsOf(spec: OpenSearchIndexSpec): Set<String> =
        if (!client.indices().existsAlias { it.name(spec.alias) }.value()) emptySet()
        else client.indices().getAlias { it.name(spec.alias) }.result().keys

    private fun String.isSearchIndex(): Boolean =
        this == projectSpec.base ||
            this == packageSpec.base ||
            startsWith("${projectSpec.base}-") ||
            startsWith("${packageSpec.base}-")
}
