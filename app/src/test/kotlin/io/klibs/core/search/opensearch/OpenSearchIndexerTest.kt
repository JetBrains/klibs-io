package io.klibs.core.search.opensearch

import BaseOpenSearchTest
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    @Qualifier("projectIndexSpec")
    private lateinit var projectSpec: OpenSearchIndexSpec

    @Autowired
    @Qualifier("packageIndexSpec")
    private lateinit var packageSpec: OpenSearchIndexSpec

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
        indexer.sync(projectSpec, at("2026-07-30T10:00:00Z"))
        indexer.sync(packageSpec, at("2026-07-30T10:00:00Z"))

        assertEquals(setOf(projectSpec.generation(at("2026-07-30T10:00:00Z"))), targetsOf(projectSpec))

        assertEquals(SEEDED_PROJECTS, countOf(projectSpec))
        assertEquals(SEEDED_PACKAGES, countOf(packageSpec))
    }

    @Test
    @Sql(value = [SEED])
    fun `a failed build leaves the alias serving the previous generation`() {
        indexer.sync(projectSpec, at("2026-07-30T10:00:00Z"))
        val serving = targetsOf(projectSpec)

        // Occupy the name the next run will use, so its create fails. The failure lands before the
        // bulk rather than mid-bulk, but the property under test — the swap never happens — is the same.
        val nextRun = at("2026-07-30T10:10:00Z")
        client.indices().create { it.index(projectSpec.generation(nextRun)) }

        assertFails { indexer.sync(projectSpec, nextRun) }

        assertEquals(serving, targetsOf(projectSpec))
        assertEquals(SEEDED_PROJECTS, countOf(projectSpec))
    }

    @Test
    @Sql(value = [SEED])
    fun `each run keeps the live generation and one previous, and drops the rest`() {
        val runs = listOf("2026-07-30T10:00:00Z", "2026-07-30T10:10:00Z", "2026-07-30T10:20:00Z").map { at(it) }
        runs.forEach { indexer.sync(projectSpec, it) }

        val expected = runs.takeLast(2).map { projectSpec.generation(it) }.toSet()
        assertEquals(expected, generationsOf(projectSpec))
        assertEquals(setOf(projectSpec.generation(runs.last())), targetsOf(projectSpec))
    }

    @Test
    @Sql(value = [SEED])
    fun `reaping drops stale foreign generations`() {
        val foreignVersion = "${projectSpec.base}-deadbeef-20200101t000000"
        client.indices().create { it.index(foreignVersion) }
        val now = Instant.now().plusSeconds(1)

        indexer.sync(projectSpec, now)

        assertFalse(client.indices().exists { it.index(foreignVersion) }.value(), "'$foreignVersion' must be reaped")
    }

    @Test
    @Sql(value = [SEED])
    fun `reaping preserves a foreign generation while an alias still serves it`() {
        val foreignVersion = "${projectSpec.base}-deadbeef-20200101t000000"
        val foreignAlias = "${projectSpec.base}-deadbeef"
        client.indices().create { it.index(foreignVersion) }
        client.indices().updateAliases { update ->
            update.actions { action -> action.add { it.index(foreignVersion).alias(foreignAlias) } }
        }
        val now = Instant.now().plusSeconds(1)

        indexer.sync(projectSpec, now)

        assertTrue(client.indices().exists { it.index(foreignVersion) }.value(), "'$foreignVersion' must survive while aliased")
    }

    @Test
    @Sql(value = [SEED])
    fun `an alias serving more than one generation fails the build before anything is created`() {
        indexer.sync(projectSpec, at("2026-07-30T10:00:00Z"))
        val extra = projectSpec.generation(at("2026-07-30T09:00:00Z"))
        client.indices().create { it.index(extra) }
        client.indices().updateAliases { update ->
            update.actions { action -> action.add { it.index(extra).alias(projectSpec.alias) } }
        }
        val targets = targetsOf(projectSpec)

        val nextRun = at("2026-07-30T10:10:00Z")
        assertFails { indexer.sync(projectSpec, nextRun) }

        assertEquals(targets, targetsOf(projectSpec))
        assertFalse(
            client.indices().exists { it.index(projectSpec.generation(nextRun)) }.value(),
            "the build must fail before creating a generation",
        )
    }

    @Test
    fun `an empty projection fails the build instead of swapping the alias onto an empty index`() {
        assertFails { indexer.sync(projectSpec, at("2026-07-30T10:00:00Z")) }

        assertFalse(indexer.aliasExists(projectSpec))
    }

    @Test
    fun `the cluster refuses to auto-create a search index`() {
        // Guarded in prod by opensearch.yml (M0); without it, a write to a deleted index silently
        // recreates it with dynamic mappings and the alias swaps onto a garbage index.
        assertFails { client.index { it.index("${projectSpec.base}-autocreate").id("1").document(mapOf("a" to 1)) } }
    }

    private fun at(iso: String): Instant = Instant.parse(iso)

    private fun targetsOf(spec: OpenSearchIndexSpec): Set<String> =
        client.indices().getAlias { it.name(spec.alias) }.result().keys

    private fun generationsOf(spec: OpenSearchIndexSpec): Set<String> =
        client.indices().get { it.index(spec.currentAliasGlob) }.result().keys
            .filter { spec.aliasMatches(it) }.toSet()

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
