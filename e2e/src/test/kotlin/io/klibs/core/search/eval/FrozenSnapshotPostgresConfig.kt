package io.klibs.core.search.eval

import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

/**
 * Postgres Testcontainer seeded from the frozen prod snapshot, exposed as a
 * `@ServiceConnection` bean so Spring Boot wires the datasource from it. Archive from
 * `SEARCH_EVAL_SNAPSHOT` (default `build/search-eval/frozen.pgdump`); missing → empty schema.
 */
@TestConfiguration(proxyBeanMethods = false)
class FrozenSnapshotPostgresConfig {

    @Bean
    @ServiceConnection
    fun frozenSnapshotPostgres(): PostgreSQLContainer<Nothing> {
        val postgres = PostgreSQLContainer<Nothing>(IMAGE).apply {
            withDatabaseName(DB)
            withUsername(DB)
            withPassword(DB)
        }
        log.info("starting {} for the search-eval regression corpus", IMAGE)
        postgres.start()
        restoreSnapshot(postgres)
        return postgres
    }

    private fun restoreSnapshot(postgres: PostgreSQLContainer<Nothing>) {
        val archive = SearchEvalData.snapshotFile()
        if (!archive.exists()) {
            log.warn(
                "snapshot missing at {} — running on empty schema, floor will be empty. " +
                    "Download it with search-eval-fetch.sh — see e2e/README.md.",
                archive.absolutePath,
            )
            return
        }
        log.info("restoring frozen snapshot {} ({} MB) via pg_restore", archive.name, archive.length() / 1_000_000)
        postgres.copyFileToContainer(MountableFile.forHostPath(archive.absolutePath), "/tmp/frozen.pgdump")
        val startedAt = System.currentTimeMillis()
        val result = postgres.execInContainer(
            "pg_restore", "--no-owner", "--no-privileges", "--clean", "--if-exists",
            "-U", DB, "-d", DB, "/tmp/frozen.pgdump",
        )
        val seconds = (System.currentTimeMillis() - startedAt) / 1000.0
        if (result.exitCode == 0) {
            log.info("pg_restore completed in {}s", "%.1f".format(seconds))
        } else {
            // Non-zero is expected: GCP-managed extensions (google_db_advisor, hypopg, …) are absent
            // in vanilla postgres — those objects are skipped, the corpus + matviews still restore.
            log.warn(
                "pg_restore exit {} in {}s (benign if only missing extensions):\n{}",
                result.exitCode, "%.1f".format(seconds), result.stderr.take(2000),
            )
        }
    }

    private companion object {
        const val DB = "klibs"
        const val IMAGE = "postgres:17.0"
        val log = LoggerFactory.getLogger(FrozenSnapshotPostgresConfig::class.java)
    }
}
