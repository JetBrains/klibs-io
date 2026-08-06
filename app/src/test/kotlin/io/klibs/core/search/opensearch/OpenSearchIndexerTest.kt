package io.klibs.core.search.opensearch

import BaseOpenSearchTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestPropertySource(properties = ["klibs.search.opensearch.foreign-reap-min-age=0s"])
class OpenSearchIndexerTest : BaseOpenSearchTest() {

    @Autowired
    private lateinit var indexer: OpenSearchIndexer

    @Autowired
    private lateinit var naming: IndexNaming

    @Autowired
    private lateinit var client: OpenSearchClient

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
    fun `sync points each alias at a single fresh generation holding every projected doc`() {
        indexer.sync(naming.project, at("2026-07-30T10:00:00Z"))
        indexer.sync(naming.packages, at("2026-07-30T10:00:00Z"))

        assertEquals(setOf(naming.project.generation(at("2026-07-30T10:00:00Z"))), targetsOf(naming.project))

        assertEquals(SEEDED_PROJECTS, countOf(naming.project))
        assertEquals(SEEDED_PACKAGES, countOf(naming.packages))
    }

    @Test
    @Sql(value = [SEED])
    fun `a failed build leaves the alias serving the previous generation`() {
        indexer.sync(naming.project, at("2026-07-30T10:00:00Z"))
        val serving = targetsOf(naming.project)

        // Occupy the name the next run will use, so its create fails. The failure lands before the
        // bulk rather than mid-bulk, but the property under test — the swap never happens — is the same.
        val nextRun = at("2026-07-30T10:10:00Z")
        client.indices().create { it.index(naming.project.generation(nextRun)) }

        assertFails { indexer.sync(naming.project, nextRun) }

        assertEquals(serving, targetsOf(naming.project))
        assertEquals(SEEDED_PROJECTS, countOf(naming.project))
    }

    @Test
    @Sql(value = [SEED])
    fun `each run keeps the live generation and one previous, and drops the rest`() {
        val runs = listOf("2026-07-30T10:00:00Z", "2026-07-30T10:10:00Z", "2026-07-30T10:20:00Z").map { at(it) }
        runs.forEach { indexer.sync(naming.project, it) }

        val expected = runs.takeLast(2).map { naming.project.generation(it) }.toSet()
        assertEquals(expected, generationsOf(naming.project))
        assertEquals(setOf(naming.project.generation(runs.last())), targetsOf(naming.project))
    }

    @Test
    @Sql(value = [SEED])
    fun `reaping drops stale foreign generations`() {
        val foreignVersion = "${naming.project.base}-deadbeef-20200101t000000"
        client.indices().create { it.index(foreignVersion) }
        val now = Instant.now().plusSeconds(1)

        indexer.sync(naming.project, now)

        assertFalse(client.indices().exists { it.index(foreignVersion) }.value(), "'$foreignVersion' must be reaped")
    }

    @Test
    @Sql(value = [SEED])
    fun `reaping preserves a foreign generation while an alias still serves it`() {
        val foreignVersion = "${naming.project.base}-deadbeef-20200101t000000"
        val foreignAlias = "${naming.project.base}-deadbeef"
        client.indices().create { it.index(foreignVersion) }
        client.indices().updateAliases { update ->
            update.actions { action -> action.add { it.index(foreignVersion).alias(foreignAlias) } }
        }
        val now = Instant.now().plusSeconds(1)

        indexer.sync(naming.project, now)

        assertTrue(client.indices().exists { it.index(foreignVersion) }.value(), "'$foreignVersion' must survive while aliased")
    }

    @Test
    @Sql(value = [SEED])
    fun `an alias serving more than one generation fails the build before anything is created`() {
        indexer.sync(naming.project, at("2026-07-30T10:00:00Z"))
        val extra = naming.project.generation(at("2026-07-30T09:00:00Z"))
        client.indices().create { it.index(extra) }
        client.indices().updateAliases { update ->
            update.actions { action -> action.add { it.index(extra).alias(naming.project.alias) } }
        }
        val targets = targetsOf(naming.project)

        val nextRun = at("2026-07-30T10:10:00Z")
        assertFails { indexer.sync(naming.project, nextRun) }

        assertEquals(targets, targetsOf(naming.project))
        assertFalse(
            client.indices().exists { it.index(naming.project.generation(nextRun)) }.value(),
            "the build must fail before creating a generation",
        )
    }

    @Test
    fun `an empty projection fails the build instead of swapping the alias onto an empty index`() {
        assertFails { indexer.sync(naming.project, at("2026-07-30T10:00:00Z")) }

        assertFalse(indexer.aliasExists(naming.project))
    }

    @Test
    fun `the cluster refuses to auto-create a search index`() {
        // Guarded in prod by opensearch.yml (M0); without it, a write to a deleted index silently
        // recreates it with dynamic mappings and the alias swaps onto a garbage index.
        assertFails { client.index { it.index("${naming.project.base}-autocreate").id("1").document(mapOf("a" to 1)) } }
    }

    private fun at(iso: String): Instant = Instant.parse(iso)

    private fun targetsOf(spec: IndexSpec): Set<String> =
        client.indices().getAlias { it.name(spec.alias) }.result().keys

    private fun generationsOf(spec: IndexSpec): Set<String> =
        client.indices().get { it.index(spec.currentAliasGlob) }.result().keys
            .filter { spec.aliasMatches(it) }.toSet()

    private fun countOf(spec: IndexSpec): Long = client.count { it.index(spec.alias) }.count()

    private fun String.isSearchIndex(): Boolean =
        this == naming.project.base ||
            this == naming.packages.base ||
            startsWith("${naming.project.base}-") ||
            startsWith("${naming.packages.base}-")

    companion object {
        private const val SEED = "classpath:sql/OpenSearchIndexerTest/seed.sql"

        /** 3 projects, one of which owns 2 of the 4 packages — so the two doc counts can't be confused. */
        private const val SEEDED_PROJECTS = 3L
        private const val SEEDED_PACKAGES = 4L
    }
}
