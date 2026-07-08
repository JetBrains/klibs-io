package io.klibs.app.eval

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

/**
 * File-backed cache of graded relevance judgments so the LLM judge runs once per query and the
 * result can be inspected / spot-corrected by hand. Persisted as JSON: `query -> {projectId: grade}`.
 */
class JudgmentStore(
    private val path: Path,
    private val objectMapper: ObjectMapper,
) {
    private val byQuery: MutableMap<String, MutableMap<Int, Int>> = mutableMapOf()

    fun load() {
        if (!Files.exists(path)) return
        val raw: Map<String, Map<String, Int>> = objectMapper.readValue(Files.readAllBytes(path))
        raw.forEach { (query, grades) ->
            byQuery[query] = grades.entries.associateTo(mutableMapOf()) { (id, grade) -> id.toInt() to grade }
        }
    }

    fun get(query: String): Map<Int, Int>? = byQuery[query]

    fun put(query: String, judgments: Map<Int, Int>) {
        byQuery[query] = judgments.toMutableMap()
    }

    fun save() {
        path.parent?.let { Files.createDirectories(it) }
        val serializable = byQuery.mapValues { (_, grades) -> grades.mapKeys { it.key.toString() } }
        Files.write(path, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(serializable))
    }
}
