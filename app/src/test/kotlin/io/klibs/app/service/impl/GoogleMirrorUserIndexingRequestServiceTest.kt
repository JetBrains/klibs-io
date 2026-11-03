package io.klibs.app.service.impl

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.configuration.properties.IndexingConfigurationProperties
import io.klibs.app.configuration.properties.IndexingConfigurationProperties.MavenCentralProperties.MavenCentralType
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.dto.MavenMetadata
import io.klibs.integration.maven.service.impl.GoogleMavenCentralMirrorStaticDataProvider
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestPropertySource(properties = ["klibs.indexing-configuration.central-sonatype.enabled=false"])
class GoogleMirrorUserIndexingRequestServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: CentralSonatypeUserIndexingRequestService

    @Autowired
    private lateinit var notifierRequestService: PluginNotifierRequestService

    @Autowired
    private lateinit var properties: IndexingConfigurationProperties

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @MockkBean
    private lateinit var googleMirrorSearchClient: GoogleMavenCentralMirrorStaticDataProvider

    @MockkBean
    private lateinit var centralSonatypeSearchClient: SonatypeCentralStaticDataProvider

    @BeforeEach
    fun setUp() {
        assertEquals(MavenCentralType.GOOGLE_MIRROR, properties.centralSonatype.type)
        assertFalse(properties.centralSonatype.enabled)
        every { googleMirrorSearchClient.scraperType } returns ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR
    }

    @Test
    @Transactional
    fun `should validate a specific version and link its mirror request to the issue`() {
        val artifact = MavenArtifact("com.example", "lib", "1.0.0", ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR)
        every { googleMirrorSearchClient.getKotlinToolingMetadata(artifact) } returns
            mockk<KotlinToolingMetadataDelegateStubImpl>()
        val issue = saveIssue("1.0.0")

        uut.discoverAndSaveRequest(requireNotNull(issue.id))

        val saved = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        assertNotNull(saved)
        assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, saved.repo)
        assertEquals(issue.id, saved.userRequestIssue?.id)
        verify(exactly = 1) { googleMirrorSearchClient.getKotlinToolingMetadata(artifact) }
    }

    @Test
    fun `should discover mirror versions without rewriting queued origin requests`() {
        val existing = indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "com.example",
                artifactId = "lib",
                version = "1.0.0",
                repo = ScraperType.CENTRAL_SONATYPE,
            )
        )
        every { googleMirrorSearchClient.getMavenMetadata("com.example", "lib") } returns
            MavenMetadata(
                "com.example", "lib",
                MavenMetadata.Versioning(versions = listOf("1.0.0", "2.0.0", "3.0.0"))
            )
        every { googleMirrorSearchClient.getKotlinToolingMetadata(any()) } answers {
            val artifact = firstArg<MavenArtifact>()
            assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, artifact.scraperType)
            if (artifact.version == "3.0.0") null else mockk<KotlinToolingMetadataDelegateStubImpl>()
        }

        uut.discoverAndSaveRequest(requireNotNull(saveIssue(null).id))

        val requests = indexingRequestRepository.findAllByGroupIdAndArtifactId("com.example", "lib")
        assertEquals(2, requests.size)
        assertEquals(existing.id, requests.single { it.version == "1.0.0" }.id)
        assertEquals(ScraperType.CENTRAL_SONATYPE, requests.single { it.version == "1.0.0" }.repo)
        assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, requests.single { it.version == "2.0.0" }.repo)
        verify(exactly = 1) { googleMirrorSearchClient.getMavenMetadata("com.example", "lib") }
    }

    @Test
    fun `notifier GAV requests should record the mirror without fetching metadata`() {
        notifierRequestService.processRequest(PluginNotifierIndexingRequest("com.example", "lib", "1.0.0"))

        val saved = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        assertNotNull(saved)
        assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, saved.repo)
        assertNull(saved.userRequestIssue)
        verify(exactly = 0) { googleMirrorSearchClient.getMavenMetadata(any(), any()) }
        verify(exactly = 0) { googleMirrorSearchClient.getKotlinToolingMetadata(any()) }
    }

    @Test
    fun `missing mirror metadata should fail without falling back to origin`() {
        every { googleMirrorSearchClient.getKotlinToolingMetadata(any()) } returns null
        every { centralSonatypeSearchClient.getKotlinToolingMetadata(any()) } returns
            mockk<KotlinToolingMetadataDelegateStubImpl>()

        val exception = assertThrows<UserRequestProcessingException> {
            uut.discoverAndSaveRequest(requireNotNull(saveIssue("1.0.0").id))
        }

        assertEquals(
            "Artifact com.example:lib:1.0.0 unavailable from the selected Maven Central source " +
                "(kotlin-tooling-metadata.json not found)",
            exception.reason,
        )
        assertEquals(0L, indexingRequestRepository.count())
    }

    private fun saveIssue(version: String?): UserRequestIssueEntity = userRequestIssueRepository.save(
        UserRequestIssueEntity(
            githubIssueNumber = 42,
            groupId = "com.example",
            artifactId = "lib",
            version = version,
        )
    )
}
