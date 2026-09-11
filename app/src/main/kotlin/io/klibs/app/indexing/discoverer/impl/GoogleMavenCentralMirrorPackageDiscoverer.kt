package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.configuration.properties.IndexingConfigurationProperties
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.project.blacklist.BlacklistRepository
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import io.klibs.integration.maven.service.MavenCentralScraper
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexScannerService
import io.klibs.integration.maven.service.impl.BaseMavenCentralStaticDataProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(IndexingConfigurationProperties.CENTRAL_SONATYPE_ENABLED, havingValue = "true")
@ConditionalOnProperty(IndexingConfigurationProperties.CENTRAL_SONATYPE_TYPE, havingValue = "GOOGLE_MIRROR")
class GoogleMavenCentralMirrorPackageDiscoverer(
    mavenIndexDownloadingService: MavenIndexDownloadingService,
    googleMavenCentralMirrorIndexScannerService: MavenIndexScannerService,
    googleMavenCentralMirrorScraper: MavenCentralScraper,
    mavenCentralLogRepository: MavenCentralLogRepository,
    packageRepository: PackageRepository,
    googleMavenCentralMirrorStaticDataProvider: BaseMavenCentralStaticDataProvider,
    blacklistRepository: BlacklistRepository,
) : BaseMavenCentralPackageDiscoverer(
    mavenIndexDownloadingService = mavenIndexDownloadingService,
    mavenIndexScannerService = googleMavenCentralMirrorIndexScannerService,
    mavenCentralScraper = googleMavenCentralMirrorScraper,
    mavenCentralLogRepository = mavenCentralLogRepository,
    packageRepository = packageRepository,
    blacklistRepository = blacklistRepository,
    fetchRemoteIndexTimestamp = googleMavenCentralMirrorStaticDataProvider::fetchRemoteIndexTimestamp,
    sourceName = "Google Maven Central mirror",
)
