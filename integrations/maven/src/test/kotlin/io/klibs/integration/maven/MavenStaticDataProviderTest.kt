package io.klibs.integration.maven

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.configuration.MavenIntegrationConfiguration
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import io.klibs.integration.maven.search.impl.GoogleMavenSearchClient
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestClient
import kotlin.test.assertNotNull

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
        fun restClientBuilder(): RestClient.Builder = RestClient.builder()

        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `test all ScraperType enum values have corresponding provider beans`() {
        val providers = applicationContext.getBeansOfType(MavenStaticDataProvider::class.java)

        // Check that we have a provider for each repository type
        ScraperType.entries.forEach { repo ->
            val providerBean = when (repo) {
                // scraper was removed, but type should still be supported, because discovered packages have it
                ScraperType.SEARCH_MAVEN -> true
                ScraperType.GOOGLE_MAVEN -> providers.entries.find { it.key == repo.name && it.value is GoogleMavenSearchClient }
                ScraperType.CENTRAL_SONATYPE -> providers.entries.find { it.key == repo.name && it.value is CentralSonatypeSearchClient }
            }
            assertNotNull(providerBean, "No provider found for repository ${repo.name}")
        }
    }
}
