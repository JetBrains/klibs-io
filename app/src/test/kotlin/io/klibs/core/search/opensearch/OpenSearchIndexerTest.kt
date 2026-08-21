package io.klibs.core.search.opensearch

import BaseOpenSearchTest
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenSearchIndexerTest : BaseOpenSearchTest() {

    @Autowired
    private lateinit var indexer: OpenSearchIndexer

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
    }

    @Test
    @Sql(value = [SEED])
    fun `sync points each alias at a single slot holding every projected doc`() {
        indexer.sync(projectSpec)
        indexer.sync(packageSpec)

        assertEquals(1, targetsOf(projectSpec).size)
        assertTrue(targetsOf(projectSpec).single() in projectSpec.slots)

        assertEquals(SEEDED_PROJECTS, countOf(projectSpec))
        assertEquals(SEEDED_PACKAGES, countOf(packageSpec))
    }

    @Test
    @Sql(value = [SEED])
    fun `consecutive builds alternate between the two slots and create nothing else`() {
        repeat(3) { indexer.sync(projectSpec) }

        assertEquals(projectSpec.slots.toSet(), indicesOf(projectSpec))
        assertEquals(projectSpec.slots[0], targetsOf(projectSpec).single())
        assertEquals(SEEDED_PROJECTS, countOf(projectSpec))
    }

    @Test
    @Sql(value = [SEED])
    fun `a rebuild overwrites the idle slot rather than the one being served`() {
        indexer.sync(projectSpec)
        val servedFirst = targetsOf(projectSpec).single()

        indexer.sync(projectSpec)

        assertEquals(projectSpec.idleSlot(servedFirst), targetsOf(projectSpec).single())
        // The previously served slot survives the rebuild: that is the rollback copy.
        assertTrue(client.indices().exists { it.index(servedFirst) }.value())
    }

    @Test
    @Sql(value = [SEED])
    fun `a failed build leaves the alias serving the previous slot and its documents`() {
        indexer.sync(projectSpec)
        val serving = targetsOf(projectSpec)

        // Empty the projection, so the next build fails after creating its index but before swapping.
        jdbcClient.sql("TRUNCATE TABLE public.project CASCADE").update()

        assertFails { indexer.sync(projectSpec) }

        assertEquals(serving, targetsOf(projectSpec))
        assertEquals(SEEDED_PROJECTS, countOf(projectSpec))
    }

    @Test
    @Sql(value = [SEED])
    fun `indices of another config hash are left alone`() {
        val foreignIndex = "${projectSpec.base}-deadbeef-0"
        client.indices().create { it.index(foreignIndex) }

        indexer.sync(projectSpec)

        assertTrue(
            client.indices().exists { it.index(foreignIndex) }.value(),
            "'$foreignIndex' belongs to another version and is cleaned up by hand, not by a build",
        )
    }

    @Test
    @Sql(value = [SEED])
    fun `an alias serving more than one slot fails the build before anything is created`() {
        indexer.sync(projectSpec)
        val idle = projectSpec.idleSlot(targetsOf(projectSpec).single())
        client.indices().create { it.index(idle) }
        client.indices().updateAliases { update ->
            update.actions { action -> action.add { it.index(idle).alias(projectSpec.alias) } }
        }
        val targets = targetsOf(projectSpec)

        assertFails { indexer.sync(projectSpec) }

        assertEquals(targets, targetsOf(projectSpec))
    }

    @Test
    fun `an empty projection fails the build instead of swapping the alias onto an empty index`() {
        assertFails { indexer.sync(projectSpec) }

        assertFalse(indexer.aliasExists(projectSpec))
    }

    @Test
    fun `the cluster refuses to auto-create a search index`() {
        // Guarded in prod by opensearch.yml (M0); without it, a write to a deleted index silently
        // recreates it with dynamic mappings and the alias swaps onto a garbage index.
        assertFails { client.index { it.index("${projectSpec.base}-autocreate").id("1").document(mapOf("a" to 1)) } }
    }

    @Test
    @Sql(value = [SEED])
    fun `the creation date of the index the alias serves is readable`() {
        val before = Instant.now().minusSeconds(60)
        indexer.sync(projectSpec)

        val createdAt = indexer.servingIndexCreatedAt(projectSpec)

        assertNotNull(createdAt)
        assertTrue(createdAt.isAfter(before), "expected $createdAt to be a just-created index")
    }

    @Test
    @Sql(value = [SEED])
    fun `the document count of the index the alias serves is readable`() {
        indexer.sync(projectSpec)

        assertEquals(countOf(projectSpec), indexer.servingDocCount(projectSpec))
    }

    @Test
    fun `an alias that serves no index has no creation date`() {
        assertNull(indexer.servingIndexCreatedAt(projectSpec))
    }

    private fun targetsOf(spec: OpenSearchIndexSpec): Set<String> =
        client.indices().getAlias { it.name(spec.alias) }.result().keys

    private fun indicesOf(spec: OpenSearchIndexSpec): Set<String> =
        client.indices().get { it.index("${spec.alias}-*") }.result().keys

    private fun countOf(spec: OpenSearchIndexSpec): Long = client.count { it.index(spec.alias) }.count()

    private fun String.isSearchIndex(): Boolean =
        this == projectSpec.base ||
            this == packageSpec.base ||
            startsWith("${projectSpec.base}-") ||
            startsWith("${packageSpec.base}-")

    companion object {
        private const val SEED = "classpath:sql/OpenSearchIndexerTest/seed.sql"

        /** 3 projects, one of which owns 2 of the 4 packages — so the two doc counts can't be confused. */
        private const val SEEDED_PROJECTS = 3L
        private const val SEEDED_PACKAGES = 4L
    }
}
