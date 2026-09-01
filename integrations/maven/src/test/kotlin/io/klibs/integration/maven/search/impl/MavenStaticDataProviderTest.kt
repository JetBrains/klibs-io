package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.sun.tools.attach.spi.AttachProvider.providers
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.configuration.MavenIntegrationConfiguration
import io.klibs.integration.maven.service.MavenStaticDataProvider
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestClient

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        MavenIntegrationConfiguration::class,
        MavenStaticDataProviderTest.MavenStaticDataProviderTestConfiguration::class]
)
class MavenStaticDataProviderTest {

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
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `test all ScraperType enum values have corresponding provider beans`() {
        val providers = applicationContext.getBeansOfType(MavenStaticDataProvider::class.java)

        assertEquals(ScraperType.entries.size, providers.size + 1)

        // Check that we have a provider for each repository type
        ScraperType.entries.forEach { repo ->
            val providerBean = when (repo) {
                // scraper was removed, but type should still be supported because discovered packages have it
                ScraperType.SEARCH_MAVEN -> true
                ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR -> providers.entries.find { ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR == it.value.scraperType }
                ScraperType.GOOGLE_MAVEN -> providers.entries.find { ScraperType.GOOGLE_MAVEN == it.value.scraperType }
                ScraperType.CENTRAL_SONATYPE -> providers.entries.find { ScraperType.CENTRAL_SONATYPE == it.value.scraperType }
            }
            assertNotNull(providerBean, "No provider found for repository ${repo.name}")
        }
    }
}
