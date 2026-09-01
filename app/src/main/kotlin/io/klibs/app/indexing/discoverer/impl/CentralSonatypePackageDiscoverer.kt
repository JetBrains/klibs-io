package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.configuration.properties.IndexingConfigurationProperties
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import io.klibs.integration.maven.service.MavenCentralScraper
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexScannerService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(IndexingConfigurationProperties.CENTRAL_SONATYPE_ENABLED, havingValue = "true")
@ConditionalOnProperty(IndexingConfigurationProperties.CENTRAL_SONATYPE_TYPE, havingValue = "ORIGIN")
class CentralSonatypePackageDiscoverer(
    mavenIndexDownloadingService: MavenIndexDownloadingService,
    centralSonatypeMavenIndexScannerService: MavenIndexScannerService,
    centralSonatypeScraper: MavenCentralScraper,
    mavenCentralLogRepository: MavenCentralLogRepository,
    packageRepository: PackageRepository,
) : BaseMavenCentralPackageDiscoverer(
    mavenIndexDownloadingService = mavenIndexDownloadingService,
    mavenIndexScannerService = centralSonatypeMavenIndexScannerService,
    mavenCentralScraper = centralSonatypeScraper,
    mavenCentralLogRepository = mavenCentralLogRepository,
    packageRepository = packageRepository,
    sourceName = "Central sonatype",
)