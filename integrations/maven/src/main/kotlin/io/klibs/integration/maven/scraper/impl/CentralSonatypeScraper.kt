package io.klibs.integration.maven.scraper.impl

import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("klibs.indexing-configuration.central-sonatype.enabled", havingValue = "true")
class CentralSonatypeScraper(
    centralSonatypeSearchClient: CentralSonatypeSearchClient,
) : BaseMavenCentralScraper(centralSonatypeSearchClient, ScraperType.CENTRAL_SONATYPE)