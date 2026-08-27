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
@ConditionalOnProperty("klibs.indexing-configuration.central-sonatype.enabled", havingValue = "true")
internal class CentralSonatypeUserIndexingRequestService(
    @Qualifier("CENTRAL_SONATYPE")
    centralSonatypeSearchClient: BaseCentralMavenSearchClient,
    indexingRequestRepository: IndexingRequestRepository,
    packageRepository: PackageRepository,
    userRequestIssueRepository: UserRequestIssueRepository,
    blacklistRepository: BlacklistRepository,
) : BaseUserIndexingRequestService(
    centralSearchClient = centralSonatypeSearchClient,
    indexingRequestRepository = indexingRequestRepository,
    packageRepository = packageRepository,
    userRequestIssueRepository = userRequestIssueRepository,
    blacklistRepository = blacklistRepository,
    scraperType = ScraperType.CENTRAL_SONATYPE,
)
