package io.klibs.app.service.impl

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.search.impl.GoogleMavenCentralMirrorSearchClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestPropertySource(
    properties = [
        "klibs.indexing-configuration.google-maven-central-mirror.enabled=true",
        "klibs.indexing-configuration.central-sonatype.enabled=false"
    ]
)
class GoogleMavenCentralMirrorUserIndexingRequestServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GoogleMavenCentralMirrorUserIndexingRequestService

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @MockitoBean
    private lateinit var googleMavenCentralMirrorSearchClient: GoogleMavenCentralMirrorSearchClient

    @Test
    fun `should save specific artifact with mirror scraper type`() {
        whenever(googleMavenCentralMirrorSearchClient.getKotlinToolingMetadata(any()))
            .thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        val issue = userRequestIssueRepository.save(
            UserRequestIssueEntity(
                githubIssueNumber = 42,
                groupId = "org.google.mirror",
                artifactId = "maven-central",
                version = "1.0.0",
            )
        )

        uut.fulfillRequest(requireNotNull(issue.id))

        val saved = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion(
            "org.google.mirror",
            "maven-central",
            "1.0.0"
        )

        assertNotNull(saved)
        assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, saved.repo)
    }
}
