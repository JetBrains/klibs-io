package io.klibs.integration.maven.service.impl

import io.klibs.integration.maven.ScraperType
import org.springframework.stereotype.Component

@Component
class GoogleMavenCentralMirrorScraper(
    googleMavenCentralMirrorSearchClient: GoogleMavenCentralMirrorStaticDataProvider,
) : BaseMavenCentralScraper(googleMavenCentralMirrorSearchClient, ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR)
