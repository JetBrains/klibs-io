package io.klibs.integration.ai

import com.openai.client.OpenAIClient
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.setup.OpenAiSetup
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.time.Duration


@Configuration
@ComponentScan(basePackages = ["io.klibs.integration.ai"])
class AiIntegrationConfiguration {

    @Bean
    @ConditionalOnProperty("klibs.ai", havingValue = "true")
    fun chatModel(
        @Value("\${spring.ai.openai.api-key}") apiKey: String
    ): OpenAiChatModel {
        return OpenAiChatModel.builder()
            .options(
                OpenAiChatOptions.builder()
                    .apiKey(apiKey)
                    .temperature(null)
                    .build()
            )
            .build()
    }

    @Bean
    @ConditionalOnProperty("klibs.ai", havingValue = "true")
    fun openAiClient(
        @Value("\${spring.ai.openai.api-key}") apiKey: String
    ): OpenAIClient {
        return OpenAiSetup.setupSyncClient(
            /* baseUrl = */ null,
            /* apiKey = */ apiKey,
            /* credential = */ null,
            /* azureDeploymentName = */ null,
            /* azureOpenAiServiceVersion = */ null,
            /* organizationId = */ null,
            /* isAzure = */ false,
            /* isGitHubModels = */ false,
            /* modelName = */ null,
            /* timeout = */ Duration.ofSeconds(120),
            /* maxRetries = */ 2,
            /* proxy = */ null,
            /* customHeaders = */ null,
            /* observationRegistry = */ ObservationRegistry.NOOP,
            /* meterRegistry = */ null,
            /* httpClientCustomizers = */ emptyList()
        )
    }
}
