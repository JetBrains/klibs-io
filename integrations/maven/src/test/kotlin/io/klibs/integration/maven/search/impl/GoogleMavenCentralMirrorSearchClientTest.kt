package io.klibs.integration.maven.search.impl

import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.ScraperType
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties = [
        "klibs.indexing-configuration.central-sonatype.enabled=false",
        "klibs.indexing-configuration.google-maven-central-mirror.enabled=true"
    ]
)
class GoogleMavenCentralMirrorSearchClientTest : BaseMavenSearchClientTest() {

    @Test
    fun `test all ScraperType enum values have corresponding provider beans`() {
        val providers = applicationContext.getBeansOfType(MavenStaticDataProvider::class.java)

        // Check that we have a provider for each repository type
        ScraperType.entries.forEach { repo ->
            val providerBean = when (repo) {
                // scraper was removed, but type should still be supported because discovered packages have it
                ScraperType.SEARCH_MAVEN -> true
                // in this test maven central client is disabled, we have a separate copy of this test
                ScraperType.CENTRAL_SONATYPE -> true
                ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR -> providers.entries.find { it.key == repo.name && it.value is GoogleMavenCentralMirrorSearchClient }
                ScraperType.GOOGLE_MAVEN -> providers.entries.find { it.key == repo.name && it.value is GoogleMavenSearchClient }
            }
            assertNotNull(providerBean, "No provider found for repository ${repo.name}")
        }
    }
}