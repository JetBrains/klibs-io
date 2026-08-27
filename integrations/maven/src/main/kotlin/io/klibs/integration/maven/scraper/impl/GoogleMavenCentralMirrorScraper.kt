package io.klibs.integration.maven.scraper.impl

import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.search.impl.GoogleMavenCentralMirrorSearchClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("klibs.indexing-configuration.google-maven-central-mirror.enabled", havingValue = "true")
class GoogleMavenCentralMirrorScraper(
    googleMavenCentralMirrorSearchClient: GoogleMavenCentralMirrorSearchClient,
) : BaseMavenCentralScraper(googleMavenCentralMirrorSearchClient, ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR)
