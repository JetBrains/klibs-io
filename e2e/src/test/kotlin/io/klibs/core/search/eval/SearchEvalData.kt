package io.klibs.core.search.eval

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

/**
 * Answer key and regression floor from `/search-eval` (test classpath), plus the previous eval run
 * from `build/` — that one is a local scratch file, deliberately not committed.
 */
object SearchEvalData {

    private val mapper = jacksonObjectMapper()

    private val lastRunFile = File("build/search-eval/last-run.json")

    fun loadCases(): List<EvalCase> = read("/search-eval/queries.json", QueriesFile::class.java).cases

    fun loadFloor(): Floor = read("/search-eval/floor.json", Floor::class.java)

    /** `-Dsearch.floor.overwrite`: record the ids passing now as the new floor. */
    fun writeFloor(ids: Collection<String>) = write("floor.json", Floor(ids.sorted(), snapshotKey()))

    fun snapshotFile(): File =
        File(System.getenv("SEARCH_EVAL_SNAPSHOT")?.takeIf { it.isNotBlank() } ?: "build/search-eval/frozen.pgdump")

    /** Bucket key of the downloaded snapshot, written by search-eval-fetch.sh. */
    fun snapshotKey(): String? = File("${snapshotFile().path}.key").takeIf { it.exists() }?.readText()?.trim()

    /** The previous eval run on this machine, or null on the first run. */
    fun loadLastRun(): EvalRunRecord? =
        lastRunFile.takeIf { it.exists() }?.let { mapper.readValue(it, EvalRunRecord::class.java) }

    fun writeLastRun(record: EvalRunRecord) {
        lastRunFile.parentFile.mkdirs()
        lastRunFile.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record))
    }

    private fun <T> read(path: String, type: Class<T>): T =
        (javaClass.getResourceAsStream(path) ?: error("resource not found: $path"))
            .use { mapper.readValue(it, type) }

    private fun write(name: String, value: Any) {
        File("src/test/resources/search-eval/$name")
            .apply { parentFile.mkdirs() }
            .writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value))
    }
}
