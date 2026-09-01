package io.klibs.integration.maven.service.impl

import io.klibs.integration.maven.ScraperType
import org.springframework.stereotype.Component

@Component
class CentralSonatypeScraper(
    centralSonatypeSearchClient: SonatypeCentralStaticDataProvider,
) : BaseMavenCentralScraper(centralSonatypeSearchClient, ScraperType.CENTRAL_SONATYPE)