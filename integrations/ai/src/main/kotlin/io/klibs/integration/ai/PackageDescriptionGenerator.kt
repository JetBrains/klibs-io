package io.klibs.integration.ai

import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
@Primary
class PackageDescriptionGenerator(
    @Value("classpath:/ai/prompts/package-description.md")
    private val packageDescriptionPrompt: Resource,
    private val aiService: AiService
) {
    fun generatePackageDescription(
        groupId: String,
        artifactId: String?,
        version: String?,
        minDescriptionWordCount: Int = 10,
        maxDescriptionWordCount: Int = 20
    ): String {
        val instructions = SystemPromptTemplate(packageDescriptionPrompt)
            .createMessage(
                mapOf(
                    "packageName" to (artifactId ?: ""),
                    "minWords" to minDescriptionWordCount,
                    "maxWords" to maxDescriptionWordCount
                )
            )
            .text.orEmpty()

        val userContent = buildString {
            append("Group ID: ${groupId}\n")
            if (artifactId != null) {
                append("Artifact ID: ${artifactId}\n")
            }
            if (version != null) {
                append("Version: ${version}\n")
            }
        }

        return cleanResponse(
            aiService.executeWebSearchRequest(
                model = AiService.WEBSEARCH_GPT,
                instructions = instructions,
                userContent = userContent,
                reasoningEffort = AiService.WEBSEARCH_EFFORT,
                methodName = "generatePackageDescription"
            )
        )
    }

    private fun cleanResponse(response: String): String {
        // Pattern to match anything in parentheses at the end of the string
        return response
            .trim()
            .replace("""\s*\(\[[^]]*]\([^)]*\)\)\s*$""".toRegex(), "").trim()
            .trim()
    }
}