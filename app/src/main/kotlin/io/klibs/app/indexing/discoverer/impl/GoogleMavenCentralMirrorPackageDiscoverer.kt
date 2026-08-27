package io.klibs.app.indexing.discoverer.impl

import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import io.klibs.integration.maven.scraper.MavenCentralScraper
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexScannerService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("klibs.indexing-configuration.google-maven-central-mirror.enabled", havingValue = "true")
class GoogleMavenCentralMirrorPackageDiscoverer(
    mavenIndexDownloadingService: MavenIndexDownloadingService,
    mavenIndexScannerService: MavenIndexScannerService,
    googleMavenCentralMirrorScraper: MavenCentralScraper,
    mavenCentralLogRepository: MavenCentralLogRepository,
    packageRepository: PackageRepository,
) : BaseMavenCentralPackageDiscoverer(
    mavenIndexDownloadingService = mavenIndexDownloadingService,
    mavenIndexScannerService = mavenIndexScannerService,
    mavenCentralScraper = googleMavenCentralMirrorScraper,
    mavenCentralLogRepository = mavenCentralLogRepository,
    packageRepository = packageRepository,
    sourceName = "Google Maven Central mirror",
)
