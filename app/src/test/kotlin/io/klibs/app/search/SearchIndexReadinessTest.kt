package io.klibs.app.search

import BaseOpenSearchTest
import io.klibs.core.search.opensearch.IndexNaming
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

private const val SEED = "classpath:sql/OpenSearchIndexerTest/seed.sql"

@TestPropertySource(properties = [
    "klibs.search.opensearch.project-index=project-readiness",
    "klibs.search.opensearch.package-index=package-readiness",
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SearchIndexReadinessTest : BaseOpenSearchTest() {

    @Autowired
    private lateinit var searchIndexSync: SearchIndexSync

    @Autowired
    private lateinit var client: OpenSearchClient

    @Autowired
    private lateinit var naming: IndexNaming

    @Autowired
    private lateinit var mockMvc: MockMvc

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
    fun `ping stays unavailable until the current aliases exist`() {
        mockMvc.get("/ping")
            .andExpect {
                status { isServiceUnavailable() }
                content { string("search index not ready") }
            }
    }

    @Test
    @Sql(value = [SEED])
    fun `ping flips ready after the first published aliases`() {
        searchIndexSync.buildMissingAliases()

        mockMvc.get("/ping")
            .andExpect {
                status { isOk() }
                content { string("pong") }
            }
    }

    @Test
    @Sql(value = [SEED])
    fun `ping stays ready after the first success even if the cluster state disappears later`() {
        searchIndexSync.buildMissingAliases()
        mockMvc.get("/ping")
            .andExpect {
                status { isOk() }
                content { string("pong") }
            }
        wipeSearchIndices()

        mockMvc.get("/ping")
            .andExpect {
                status { isOk() }
                content { string("pong") }
            }
    }

    private fun String.isSearchIndex(): Boolean =
        this == naming.project.base ||
            this == naming.packages.base ||
            startsWith("${naming.project.base}-") ||
            startsWith("${naming.packages.base}-")
}
