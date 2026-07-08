package io.klibs.app.eval

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.integration.ai.AiService
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.stereotype.Component

/**
 * LLM-as-judge: grades how well each pooled candidate project satisfies a query intent, on a
 * 0-3 scale (0 = irrelevant, 3 = perfect). Grading the whole pool in one request keeps the judge
 * consistent across candidates and cheap. Candidates the model omits default to grade 0.
 */
@Component
class RelevanceJudge(
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
) {
    fun judge(query: String, candidates: List<SearchProjectResult>): Map<Int, Int> {
        if (candidates.isEmpty()) return emptyMap()

        val messages = buildList<Message> {
            add(SystemMessage(SYSTEM_PROMPT))
            add(UserMessage(renderCandidates(query, candidates)))
        }
        val content = aiService.executeOpenAiRequest(Prompt(messages, options), "judgeRelevance", AiService.DEFAULT_GPT)
        val parsed: GradeList = objectMapper.readValue(content)

        val graded = parsed.grades.associate { it.id to it.grade.coerceIn(0, 3) }
        return candidates.associate { it.id to (graded[it.id] ?: 0) }
    }

    private fun renderCandidates(query: String, candidates: List<SearchProjectResult>): String = buildString {
        appendLine("QUERY: $query")
        appendLine()
        appendLine("CANDIDATE PROJECTS (grade each by project id):")
        candidates.forEach { c ->
            val tags = c.tags.joinToString(", ")
            val description = c.description?.take(MAX_DESCRIPTION_CHARS).orEmpty()
            appendLine("- id=${c.id} | name=${c.name} | repo=${c.ownerLogin}/${c.repoName} | tags=[$tags] | $description")
        }
    }

    data class GradeList(val grades: List<Grade>)
    data class Grade(val id: Int, val grade: Int)

    private val options by lazy {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "grades" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "id" to mapOf("type" to "integer"),
                            "grade" to mapOf("type" to "integer", "description" to "0=irrelevant..3=perfect"),
                        ),
                        "required" to listOf("id", "grade"),
                        "additionalProperties" to false,
                    ),
                ),
            ),
            "required" to listOf("grades"),
            "additionalProperties" to false,
        )
        val jsonSchema = ResponseFormat.JsonSchema.builder().name("relevance_grades").schema(schema).strict(true).build()
        val responseFormat = ResponseFormat.builder().type(ResponseFormat.Type.JSON_SCHEMA).jsonSchema(jsonSchema).build()
        OpenAiChatOptions.builder().model(AiService.DEFAULT_GPT).responseFormat(responseFormat).temperature(null).build()
    }

    private companion object {
        const val MAX_DESCRIPTION_CHARS = 300
        val SYSTEM_PROMPT = """
            You grade search results for klibs.io, a catalog of Kotlin Multiplatform libraries.
            For each candidate project decide how well it satisfies the developer's search query intent.
            Grades: 0 = irrelevant, 1 = marginally related, 2 = relevant, 3 = a perfect match.
            Judge by capability/topic, not by name similarity. Return a grade for every candidate id.
        """.trimIndent()
    }
}
