package io.klibs.integration.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml

@Service
class ProjectTagsGenerator(
    @Value("classpath:/ai/prompts/project-tags.md")
    private val tagsPrompt: Resource,
    @Value("classpath:/ai/prompts/tag_rules.yaml")
    private val tagRulesResource: Resource,
    private val aiService: AiService,
    private val objectMapper: ObjectMapper
) {
    fun generateTagsForProject(
        projectName: String,
        projectDescription: String,
        repoDescription: String,
        readmeMdContent: String
    ): List<String> {
        // If there is no README, no description, and no repository description, skip generation
        if (projectDescription.isBlank() && repoDescription.isBlank() && readmeMdContent.isBlank()) {
            throw IllegalStateException("Skip: no description, no repo description and no README available")
        }

        val sb = StringBuilder()
        sb.appendLine("Project name: $projectName")
        if (projectDescription.isNotBlank()) sb.appendLine("Description: $projectDescription")
        if (repoDescription.isNotBlank()) sb.appendLine("Repository description: $repoDescription")
        if (readmeMdContent.isNotBlank()) sb.appendLine("README: $readmeMdContent")
        val userContent = sb.toString().trim()

        val messages = buildList<Message> {
            add(SystemMessage(systemPrompt))
            add(UserMessage(userContent))
        }

        val prompt = Prompt(messages, options)

        val content = aiService.executeOpenAiRequest(
            prompt,
            "generateProjectTags",
            modelName
        )

        val parsed: TagSelection = objectMapper.readValue(content)

        val picked = parsed.indices.mapNotNull { idx -> tagRules.getOrNull(idx)?.name }
        return picked
    }

    data class TagRule(
        val name: String,
        val definition: String?,
        val positive_cues: List<String>?,
        val hard_negatives: List<String>?,
        val synonyms: List<String>?,
        val tag_synonyms: List<String>?,
    )

    data class TagSelection(val indices: List<Int>)

    private val schema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "indices" to mapOf(
                "type" to "array",
                "items" to mapOf("type" to "integer"),
                "description" to "Indices of selected tags from the ALLOWED TAGS list"
            )
        ),
        "required" to listOf("indices"),
        "additionalProperties" to false
    )

    private val modelName = AiService.DEFAULT_GPT

    private val options by lazy {
        val jsonSchema = ResponseFormat.JsonSchema.builder()
            .name("tag_indices")
            .schema(schema)
            .strict(true)
            .build()

        val responseFormat = ResponseFormat.builder()
            .type(ResponseFormat.Type.JSON_SCHEMA)
            .jsonSchema(jsonSchema)
            .build()

        OpenAiChatOptions.builder()
            .model(modelName)
            .responseFormat(responseFormat)
            .temperature(null)
            .build()
    }

    private val tagRules: List<TagRule> by lazy {
        val yaml = Yaml()

        tagRulesResource.inputStream.use { input ->
            val root = yaml.load<Any>(input)

            val map = root as? Map<*, *>
            val rules = (map?.get("tag_rules") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            rules.map { ruleMap ->
                TagRule(
                    name = ruleMap["name"] as? String ?: "",
                    definition = ruleMap["definition"] as? String,
                    positive_cues = (ruleMap["positive_cues"] as? List<*>)?.mapNotNull { it as? String },
                    hard_negatives = (ruleMap["hard_negatives"] as? List<*>)?.mapNotNull { it as? String },
                    synonyms = (ruleMap["synonyms"] as? List<*>)?.mapNotNull { it as? String },
                    tag_synonyms = (ruleMap["tag_synonyms"] as? List<*>)?.mapNotNull { it as? String },
                )
            }
        }
    }

    private val tagRulesString: String by lazy {
        tagRules.withIndex().joinToString("\n") { (i, r) ->
            val def = r.definition ?: ""
            val pos = (r.positive_cues ?: emptyList()).joinToString(", ", prefix = "[", postfix = "]") { it }
            val neg = (r.hard_negatives ?: emptyList()).joinToString(", ", prefix = "[", postfix = "]") { it }
            val syn = (r.synonyms ?: emptyList()).joinToString(", ", prefix = "[", postfix = "]") { it }
            "${i}:{name:\"${r.name}\", definition:\"${def.replace("\"","\\\"")}\", positive_cues:${pos}, hard_negatives:${neg}, synonyms:${syn}}"
        }
    }

    private val prompt: String by lazy {
        tagsPrompt.inputStream.use { input ->
            input.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }

    private val systemPrompt: String by lazy {
       StringBuilder().apply {
            append(prompt.trim())
            append("\n\nALLOWED TAGS (NUMBERED OBJECTS)\n")
            append(tagRulesString)
        }.toString()
    }
}
