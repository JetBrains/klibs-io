package io.klibs.integration.maven

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [MavenIntegrationProperties::class])
@ComponentScan(basePackages = ["io.klibs.integration.maven"])
class MavenIntegrationConfiguration {

    @OptIn(ExperimentalXmlUtilApi::class)
    @Bean
    fun xml() = XML {
        autoPolymorphic = true
        defaultPolicy {
            unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList() }
        }
    }
}
