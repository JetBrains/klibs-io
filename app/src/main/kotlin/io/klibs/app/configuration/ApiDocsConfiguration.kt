package io.klibs.app.configuration

import io.klibs.app.configuration.properties.ApiDocsProperties
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiDocsConfiguration(
    private val apiDocsProperties: ApiDocsProperties
) {
    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title(apiDocsProperties.title)
                    .description(apiDocsProperties.description)
            )
            .servers(apiDocsProperties.serverUrls.map { Server().url(it) })
            .components(Components().addBasicAuth())
            .addSecurityItem(SecurityRequirement().addList("Authorization", listOf("read", "write")))
    }

    private fun Components.addBasicAuth(): Components {
        return this.addSecuritySchemes(
            "Authorization",
            SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("Basic")
                .bearerFormat("Basic")
                .`in`(SecurityScheme.In.HEADER)
                .name("Authorization")
        )
    }
}
