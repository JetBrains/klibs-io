package io.klibs.integration.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("klibs.ai-models")
data class AiModelsProperties(
    val default: String = "gpt-5-mini",
    val webSearch: String = "gpt-5-mini",
    val webSearchEffort: String = "low",
)
