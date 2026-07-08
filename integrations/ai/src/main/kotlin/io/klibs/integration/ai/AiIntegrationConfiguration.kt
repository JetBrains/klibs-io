package io.klibs.integration.ai

import org.springframework.ai.document.MetadataMode
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient


@Configuration
@ComponentScan(basePackages = ["io.klibs.integration.ai"])
class AiIntegrationConfiguration {

    @Bean
    @ConditionalOnProperty("klibs.ai", havingValue = "true")
    fun chatModel(
        @Value("\${spring.ai.openai.api-key}") apiKey: String,
        restClient: RestClient.Builder
    ): OpenAiChatModel {
        return OpenAiChatModel.builder()
            .openAiApi(
                OpenAiApi.builder()
                    .apiKey(apiKey)
                    .restClientBuilder(restClient)
                    .build()
            )
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .temperature(null)
                    .build()
            )
            .build()
    }

    @Bean
    @ConditionalOnProperty("klibs.ai", havingValue = "true")
    fun embeddingModel(
        @Value("\${spring.ai.openai.api-key}") apiKey: String,
        restClient: RestClient.Builder
    ): OpenAiEmbeddingModel {
        return OpenAiEmbeddingModel(
            OpenAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClient)
                .build(),
            MetadataMode.EMBED,
            // No default dimensions: Spring AI merges the default into every request and cannot
            // clear it at runtime, so a default here would be sent to text-embedding-ada-002 (which
            // rejects `dimensions`). text-embedding-3-small already returns 1536 dims natively.
            OpenAiEmbeddingOptions.builder()
                .model(AiService.EMBEDDING_MODEL)
                .build()
        )
    }
}
