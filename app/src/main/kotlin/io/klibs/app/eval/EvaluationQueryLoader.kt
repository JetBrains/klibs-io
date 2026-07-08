package io.klibs.app.eval

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

/** A concept query together with how often it appeared in the raw search log. */
data class WeightedQuery(val text: String, val frequency: Int)

/**
 * Loads the committed concept query set (`eval/concept-queries.txt`, produced by
 * `scripts/eval/extract_concept_queries.py`) and returns the most frequent queries.
 * Lines are `query<TAB>frequency`; `#` comment lines and blanks are ignored.
 */
@Component
class EvaluationQueryLoader(
    @Value("classpath:eval/concept-queries.txt")
    private val resource: Resource,
) {
    fun load(limit: Int): List<WeightedQuery> =
        resource.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    val text = parts.getOrNull(0)?.trim().orEmpty()
                    if (text.isEmpty()) return@mapNotNull null
                    WeightedQuery(text, parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1)
                }
                .toList()
        }
            .sortedByDescending { it.frequency }
            .take(limit)
}
