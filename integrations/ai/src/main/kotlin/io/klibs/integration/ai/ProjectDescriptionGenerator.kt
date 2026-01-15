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
class ProjectDescriptionGenerator(
    @Value("classpath:/ai/prompts/project-description.md")
    private val projectDescriptionPrompt: Resource,
    private val aiService: AiService
) {
    fun generateProjectDescription(
        projectName: String,
        readmeMdContent: String,
        minDescriptionWordCount: Int = 20,
        maxDescriptionWordCount: Int = 30
    ): String {
        val systemMessage = SystemPromptTemplate(projectDescriptionPrompt)
            .createMessage(
                mapOf(
                    "projectName" to projectName,
                    "minWords" to minDescriptionWordCount,
                    "maxWords" to maxDescriptionWordCount
                )
            )

        val options = OpenAiChatOptions.builder()
            .model(AiService.DEFAULT_GPT)
            .build()

        val userMessage = UserMessage(readmeMdContent)
        val prompt = Prompt(listOf(systemMessage, userMessage), options)

        return aiService.executeOpenAiRequest(prompt, "generateProjectDescription", AiService.DEFAULT_GPT)
    }
}