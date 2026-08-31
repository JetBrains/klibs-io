package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.configuration.MavenIntegrationConfiguration
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestClient
import kotlin.test.assertNotNull
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ContextConfiguration(
    classes = [
        MavenIntegrationConfiguration::class,
        BaseMavenSearchClientTest.MavenStaticDataProviderTestConfiguration::class]
)
@TestPropertySource(
    properties = [
        "klibs.integration.maven.central.rateLimitCapacity=100",
        "klibs.integration.maven.central.rateLimitRefillAmount=100",
        "klibs.integration.maven.central.rateLimitRefillPeriodSec=5",
        "klibs.integration.maven.central.indexEndpoint=http://localhost:8080/index",
        "klibs.integration.maven.central.indexDir=/tmp/maven-index",
        "klibs.integration.maven.central.contentEndpoint=http://localhost:8080/content/",
        "klibs.integration.maven.central.contentFallbackEndpoint=http://localhost:8080/fallback/",
    ]
)

class BaseMavenSearchClientTest {

    @Configuration
    class MavenStaticDataProviderTestConfiguration {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()

        @Bean
        fun xmlMapper(): XmlMapper = XmlMapper()

        @Bean
        fun restClientBuilder(): RestClient.Builder = RestClient.builder()

        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }

    @Autowired
    protected lateinit var applicationContext: ApplicationContext
}
