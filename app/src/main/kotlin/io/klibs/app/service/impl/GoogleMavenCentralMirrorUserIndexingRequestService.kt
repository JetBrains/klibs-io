package io.klibs.app.service.impl

import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.core.project.blacklist.BlacklistRepository
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.search.impl.BaseCentralMavenSearchClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("klibs.indexing-configuration.google-maven-central-mirror.enabled", havingValue = "true")
internal class GoogleMavenCentralMirrorUserIndexingRequestService(
    @Qualifier("GOOGLE_MAVEN_CENTRAL_MIRROR")
    private val centralSearchClient: BaseCentralMavenSearchClient,
    indexingRequestRepository: IndexingRequestRepository,
    packageRepository: PackageRepository,
    userRequestIssueRepository: UserRequestIssueRepository,
    blacklistRepository: BlacklistRepository,
) : BaseUserIndexingRequestService(
    centralSearchClient = centralSearchClient,
    indexingRequestRepository = indexingRequestRepository,
    packageRepository = packageRepository,
    userRequestIssueRepository = userRequestIssueRepository,
    blacklistRepository = blacklistRepository,
    scraperType = ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR,
)
