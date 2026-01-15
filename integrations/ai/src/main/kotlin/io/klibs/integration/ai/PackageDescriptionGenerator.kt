package io.klibs.integration.ai

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.openai.OpenAiChatOptions
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
        packageName: String,
        groupId: String,
        artifactId: String?,
        version: String?,
        minDescriptionWordCount: Int = 10,
        maxDescriptionWordCount: Int = 20
    ): String {
        val systemMessage = SystemPromptTemplate(packageDescriptionPrompt)
            .createMessage(
                mapOf(
                    "packageName" to packageName,
                    "minWords" to minDescriptionWordCount,
                    "maxWords" to maxDescriptionWordCount
                )
            )

        val userContent = buildString {
            append("Group ID: ${groupId}\n")
            if (artifactId != null) {
                append("Artifact ID: ${artifactId}\n")
            }
            if (version != null) {
                append("Version: ${version}\n")
            }
        }

        val userMessage = UserMessage(userContent)

        val options = OpenAiChatOptions.builder()
            .model(AiService.WEBSEARCH_GPT)
            .build()

        val prompt = Prompt(listOf(systemMessage, userMessage), options)

        return cleanResponse(
            aiService.executeOpenAiRequest(
                prompt,
                "generatePackageDescription",
                AiService.WEBSEARCH_GPT
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