package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.UserRequestIndexingStatus
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageIndexRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.core.pckg.repository.UserRequestReportRepository
import io.klibs.app.configuration.IndexingRetryConfiguration
import io.klibs.core.pckg.service.PackageDescriptionService
import io.klibs.core.readme.ReadmeContentBuilder
import io.klibs.integration.ai.PackageDescriptionGenerator
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.maven.MavenPom
import io.klibs.integration.maven.PomWithReleaseDate
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.androidx.Variant
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import org.apache.maven.model.Scm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(OutputCaptureExtension::class)
class PackageIndexingServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: PackageIndexingService

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Autowired
    private lateinit var packageIndexRepository: PackageIndexRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var packageDescriptionService: PackageDescriptionService

    @Autowired
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @Autowired
    private lateinit var userRequestReportRepository: UserRequestReportRepository

    @Autowired
    private lateinit var indexingRetryConfiguration: IndexingRetryConfiguration

    @MockitoBean
    private lateinit var mavenStaticDataProvider: CentralSonatypeSearchClient

    @MockitoBean
    private lateinit var packageDescriptionGenerator: PackageDescriptionGenerator

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockitoBean
    private lateinit var readmeContentBuilder: ReadmeContentBuilder

    @Test
    fun `should return false when queue is empty`(output: CapturedOutput) {
        assertNull(indexingRequestRepository.findFirstForIndexing())

        val result = uut.processPackageQueue()

        assertFalse(result, "Should return false when queue is empty")
        assertContains(output.out, "The package index queue is empty")
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-request-for-processing.sql"])
    fun `should handle an exceptions during processing and return true`(output: CapturedOutput) {
        val packageIndexRequestBeforeProcessing = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(packageIndexRequestBeforeProcessing)

        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any())).thenThrow(RuntimeException("Mocked getPom exception"))

        val result = uut.processPackageQueue()

        assertTrue(result, "Should return true when a request is processed")
        assertContains(output.out, "Error during claiming an indexing request")

        // Verify the failed_attempts count is incremented
        val failedAttempts = jdbcTemplate.queryForObject(
            "SELECT failed_attempts FROM package_index_request WHERE id = ${packageIndexRequestBeforeProcessing.idNotNull}",
            Int::class.java
        )
        assertEquals(1, failedAttempts, "Failed attempts should be incremented")
        assertContains(output.out, "Mocked getPom exception")
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-request-for-processing.sql"])
    fun `should successfully process package indexing request`(output: CapturedOutput) {

        val packageIndexRequestBeforeProcessing = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(packageIndexRequestBeforeProcessing)

        val pom = mock<MavenPom>()
        whenever(pom.groupId).thenReturn(packageIndexRequestBeforeProcessing.groupId)
        whenever(pom.artifactId).thenReturn(packageIndexRequestBeforeProcessing.artifactId)
        whenever(pom.version).thenReturn(packageIndexRequestBeforeProcessing.version)
        val kotlinToolingMetadata = mock<GradleMetadata>()
        whenever(kotlinToolingMetadata.variants).thenReturn(listOf(Variant(mapOf("org.jetbrains.kotlin.platform.type" to "js"))))
        val kotlinToolingMetadataDelegate = KotlinToolingMetadataDelegateStubImpl(kotlinToolingMetadata)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any())).thenReturn(
            PomWithReleaseDate(
                pom,
                java.time.Instant.now()
            )
        )
        whenever(mavenStaticDataProvider.getKotlinToolingMetadata(any())).thenReturn(kotlinToolingMetadataDelegate)

        val result = uut.processPackageQueue()

        assertTrue(result, "Should return true")
        assertFalse(output.out.contains("Unable to process the index request"))

        assertNull(
            indexingRequestRepository.findFirstForIndexing(),
            "Processed request should be removed from the queue"
        )
        val foundPackages = packageRepository.findByGroupIdAndArtifactIdOrderByReleaseTsDesc(
            packageIndexRequestBeforeProcessing.groupId,
            packageIndexRequestBeforeProcessing.artifactId
        )
        assertEquals(1, foundPackages.size)
        assertEquals(foundPackages.get(0).groupId, packageIndexRequestBeforeProcessing.groupId)
        assertEquals(foundPackages.get(0).artifactId, packageIndexRequestBeforeProcessing.artifactId)
        assertEquals(foundPackages.get(0).version, packageIndexRequestBeforeProcessing.version)
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-request-for-processing.sql"])
    fun `should parse androidJvm platform when jvm target is KotlinMultiplatformAndroidLibraryTargetImpl`() {
        val indexRequest = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(indexRequest)

        val pom = mock<MavenPom>()
        whenever(pom.groupId).thenReturn(indexRequest.groupId)
        whenever(pom.artifactId).thenReturn(indexRequest.artifactId)
        whenever(pom.version).thenReturn(indexRequest.version)

        // Gradle metadata stub that mimics KMP Android target reported under JVM with AGP class name
        val kotlinToolingMetadata = mock<GradleMetadata>()
        whenever(kotlinToolingMetadata.variants).thenReturn(
            listOf(
                Variant(
                    mapOf(
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                        // This value should be copied into KotlinToolingMetadata.ProjectTargetMetadata.target by the stub
                        "org.jetbrains.kotlin.native.target" to "com.android.build.api.variant.impl.KotlinMultiplatformAndroidLibraryTargetImpl"
                    )
                )
            )
        )
        val kotlinToolingMetadataDelegate = KotlinToolingMetadataDelegateStubImpl(kotlinToolingMetadata)

        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any())).thenReturn(
            PomWithReleaseDate(
                pom,
                java.time.Instant.now()
            )
        )
        whenever(mavenStaticDataProvider.getKotlinToolingMetadata(any())).thenReturn(kotlinToolingMetadataDelegate)

        // Act
        val result = uut.processPackageQueue()

        // Assert basic processing
        assertTrue(result, "Should return true")
        assertNull(
            indexingRequestRepository.findFirstForIndexing(),
            "Processed request should be removed from the queue"
        )

        // Verify that target was parsed as ANDROIDJVM with fallback target compatibility 1.8
        val saved = packageRepository.findByGroupIdAndArtifactIdAndVersion(
            indexRequest.groupId,
            indexRequest.artifactId,
            requireNotNull(indexRequest.version)
        )
        assertNotNull(saved, "Saved package should be present")

        val targets = jdbcTemplate.query(
            "SELECT platform, target FROM package_target WHERE package_id = ?",
            { rs, _ -> rs.getString("platform") to rs.getString("target") },
            saved.id
        )

        assertEquals(1, targets.size, "Exactly one target expected")
        val (platform, target) = targets.first()
        assertEquals("ANDROIDJVM", platform, "Platform should be ANDROIDJVM when AGP JVM Android target is detected")
        assertEquals("1.8", target, "Android targetCompatibility should fallback to 1.8")
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-package-with-generated-description.sql"])
    fun `should generate new description when indexing new version of package with generated description`(output: CapturedOutput) {
        // First, generate a description for the existing package to set generatedDescription to true
        val groupId = "com.example"
        val artifactId = "test-library-gen"
        val version1 = "1.0.0"
        val version2 = "2.0.0"
        val generatedDescription = "This is a generated description for testing"

        // Mock the AI service to return a predictable description
        whenever(
            packageDescriptionGenerator.generatePackageDescription(
                any(), // groupId
                any(), // artifactId
                any(), // version
                any(), // minDescriptionWordCount
                any()  // maxDescriptionWordCount
            )
        ).thenReturn(generatedDescription)

        // Generate a description for the existing package
        packageDescriptionService.generateDescription(groupId, artifactId, version1)

        // Verify that the package now has generatedDescription set to true
        val packageBeforeIndexing =
            packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version1)
        assertNotNull(packageBeforeIndexing, "Package should exist")
        assertTrue(packageBeforeIndexing.generatedDescription, "Package should have generatedDescription set to true")

        // Backdate the previous generation beyond the regen TTL so a genuinely new version still regenerates.
        jdbcTemplate.update(
            "UPDATE package SET description_generated_at = ? WHERE group_id = ? AND artifact_id = ? AND version = ?",
            java.sql.Timestamp.from(Instant.now().minus(120, ChronoUnit.DAYS)),
            groupId, artifactId, version1
        )

        // Set up mocks for processing the indexing request
        val packageIndexRequest = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(packageIndexRequest, "Indexing request should exist")
        assertEquals(groupId, packageIndexRequest.groupId)
        assertEquals(artifactId, packageIndexRequest.artifactId)
        assertEquals(version2, packageIndexRequest.version)

        val pom = mock<MavenPom>()
        whenever(pom.groupId).thenReturn(groupId)
        whenever(pom.artifactId).thenReturn(artifactId)
        whenever(pom.version).thenReturn(version2)
        val kotlinToolingMetadata = mock<GradleMetadata>()
        whenever(kotlinToolingMetadata.variants).thenReturn(listOf(Variant(mapOf("org.jetbrains.kotlin.platform.type" to "js"))))
        val kotlinToolingMetadataDelegate = KotlinToolingMetadataDelegateStubImpl(kotlinToolingMetadata)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any())).thenReturn(
            PomWithReleaseDate(
                pom,
                java.time.Instant.now()
            )
        )
        whenever(mavenStaticDataProvider.getKotlinToolingMetadata(any())).thenReturn(kotlinToolingMetadataDelegate)

        // Process the indexing request
        val result = uut.processPackageQueue()

        // Verify that the request was processed successfully
        assertTrue(result, "Should return true")
        assertFalse(output.out.contains("Unable to process the index request"))
        assertNull(
            indexingRequestRepository.findFirstForIndexing(),
            "Processed request should be removed from the queue"
        )

        // Verify that a new package was created with the new version
        val packages = packageRepository.findByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId, artifactId)
        assertEquals(2, packages.size, "Should have two packages with the same groupId and artifactId")

        // Verify that the new package has the generated description
        val newPackage = packages.first { it.version == version2 }
        assertEquals(generatedDescription, newPackage.description, "New package should have the generated description")
        assertTrue(newPackage.generatedDescription, "New package should have generatedDescription set to true")
        assertNotNull(newPackage.descriptionGeneratedAt, "Generated description must record description_generated_at")

        // Verify that the log contains a message about generating a new description
        assertContains(
            output.out,
            "Generated new description for $groupId:$artifactId:$version2 because previous version had a generated description"
        )
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-reindex-request-with-generated-description.sql"])
    fun `reindex preserves the existing generated description instead of overwriting it`() {
        val groupId = "com.example"
        val artifactId = "test-library-reindex"
        val version = "1.0.0"

        val request = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(request)
        assertTrue(request.reindex, "Seeded request must be a reindex request")

        val before = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        assertNotNull(before)
        val generatedAtBefore = before.descriptionGeneratedAt
        assertNotNull(generatedAtBefore, "Precondition: seeded package has a generated timestamp")

        // If the AI generator were ever invoked its result would be this sentinel; it must never reach the DB.
        whenever(packageDescriptionGenerator.generatePackageDescription(any(), any(), any(), any(), any()))
            .thenReturn("AI SENTINEL - must not be persisted on reindex")

        stubMavenFetch(groupId, artifactId, version, pomDescription = "Fresh POM description")

        uut.processRequest(request.idNotNull)

        val updated = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        assertNotNull(updated)
        assertEquals(
            "This is a generated description for version 1.0.0", updated.description,
            "Reindex must keep the existing description, not the POM nor a freshly generated one"
        )
        assertTrue(updated.generatedDescription, "Reindex must preserve the generated flag")
        assertEquals(generatedAtBefore, updated.descriptionGeneratedAt, "Reindex must preserve description_generated_at")
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-older-version-request-with-generated-latest.sql"])
    fun `indexing an older non-latest version keeps the POM description and does not generate`() {
        val groupId = "com.example"
        val artifactId = "test-library-older"
        val olderVersion = "1.0.0"

        // Sentinel that would only appear if a regeneration wrongly fired for this non-latest version.
        whenever(packageDescriptionGenerator.generatePackageDescription(any(), any(), any(), any(), any()))
            .thenReturn("AI SENTINEL - must not be persisted for a non-latest version")

        stubMavenFetch(groupId, artifactId, olderVersion, pomDescription = "Original POM description")

        val request = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(request)
        uut.processRequest(request.idNotNull)

        val newPackage = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, olderVersion)
        assertNotNull(newPackage)
        assertEquals(
            "Original POM description", newPackage.description,
            "A non-latest version must take the POM description, never a generated one"
        )
        assertFalse(newPackage.generatedDescription, "Older version must not be marked as generated")
        assertNull(newPackage.descriptionGeneratedAt, "Non-generated description must not record a timestamp")
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-recent-generated-latest.sql"])
    fun `new version does not regenerate when the previous description is within the regen TTL`() {
        val groupId = "com.example"
        val artifactId = "test-library-ttl"
        val newVersion = "2.0.0"

        val previousLatest = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, "1.0.0")
        assertNotNull(previousLatest)
        val previousGeneratedAt = previousLatest.descriptionGeneratedAt
        assertNotNull(previousGeneratedAt, "Precondition: previous latest was generated recently (within TTL)")

        // Sentinel that must not be persisted: a regen within the TTL window is disallowed.
        whenever(packageDescriptionGenerator.generatePackageDescription(any(), any(), any(), any(), any()))
            .thenReturn("AI SENTINEL - must not be persisted within TTL")

        stubMavenFetch(groupId, artifactId, newVersion, pomDescription = "Fresh POM description")

        val request = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(request)
        uut.processRequest(request.idNotNull)

        // Within TTL the previous generated description is carried forward instead of regenerated.
        val indexed = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, newVersion)
        assertNotNull(indexed)
        assertEquals("Recent AI description", indexed.description, "Description must be carried forward, not regenerated")
        assertTrue(indexed.generatedDescription, "Carried-forward description keeps the generated flag")
        assertEquals(previousGeneratedAt, indexed.descriptionGeneratedAt, "TTL carry-forward must keep the original timestamp")
    }

    /**
     * Stubs the Maven static-data boundary (POM + release date + tooling metadata) so a queued
     * request can be processed end-to-end against the real database without network access.
     */
    private fun stubMavenFetch(groupId: String, artifactId: String, version: String, pomDescription: String?) {
        val pom = mock<MavenPom>()
        whenever(pom.groupId).thenReturn(groupId)
        whenever(pom.artifactId).thenReturn(artifactId)
        whenever(pom.version).thenReturn(version)
        whenever(pom.description).thenReturn(pomDescription)
        val kotlinToolingMetadata = mock<GradleMetadata>()
        whenever(kotlinToolingMetadata.variants)
            .thenReturn(listOf(Variant(mapOf("org.jetbrains.kotlin.platform.type" to "js"))))
        val kotlinToolingMetadataDelegate = KotlinToolingMetadataDelegateStubImpl(kotlinToolingMetadata)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any()))
            .thenReturn(PomWithReleaseDate(pom, Instant.now()))
        whenever(mavenStaticDataProvider.getKotlinToolingMetadata(any())).thenReturn(kotlinToolingMetadataDelegate)
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexingServiceTest/insert-request-for-processing.sql"])
    fun `should markAsFailed when ReadmeContentBuilder buildFromMarkdown throws exception`(output: CapturedOutput) {
        val packageIndexRequest = indexingRequestRepository.findFirstForIndexing()
        assertNotNull(packageIndexRequest)

        val ownerLogin = "test-owner"
        val repoName = "test-repo"
        val repoNativeId = 12345L
        val ownerNativeId = 67890L

        val pom = mock<MavenPom>()
        whenever(pom.groupId).thenReturn(packageIndexRequest.groupId)
        whenever(pom.artifactId).thenReturn(packageIndexRequest.artifactId)
        whenever(pom.version).thenReturn(packageIndexRequest.version)
        val scm = Scm()
        scm.url = "https://github.com/$ownerLogin/$repoName"
        whenever(pom.scm).thenReturn(scm)

        val kotlinToolingMetadata = mock<GradleMetadata>()
        whenever(kotlinToolingMetadata.variants).thenReturn(listOf(Variant(mapOf("org.jetbrains.kotlin.platform.type" to "js"))))
        val kotlinToolingMetadataDelegate = KotlinToolingMetadataDelegateStubImpl(kotlinToolingMetadata)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any())).thenReturn(
            PomWithReleaseDate(
                pom,
                Instant.now()
            )
        )
        whenever(mavenStaticDataProvider.getKotlinToolingMetadata(any())).thenReturn(kotlinToolingMetadataDelegate)

        // Mock GitHub integration to successfully create SCM entities
        val ghRepo = GitHubRepository(
            nativeId = repoNativeId,
            name = repoName,
            owner = ownerLogin,
            defaultBranch = "main",
            createdAt = Instant.now(),
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            stars = 10,
            lastActivity = Instant.now(),
        )
        whenever(gitHubIntegration.getRepository(ownerLogin, repoName)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getUser(ownerLogin)).thenReturn(
            GitHubUser(
                id = ownerNativeId,
                login = ownerLogin,
                type = "User",
                name = "Test Owner",
                company = null,
                blog = null,
                location = null,
                email = null,
                bio = null,
                twitterUsername = null,
                followers = 0,
            )
        )
        whenever(gitHubIntegration.getLicense(repoNativeId)).thenReturn(null)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(eq(repoNativeId), any()))
            .thenReturn(ReadmeFetchResult.Content("# Test README"))

        // Mock ReadmeContentBuilder to throw an exception
        whenever(readmeContentBuilder.buildFromMarkdown(any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("Mocked buildFromMarkdown exception"))

        val result = uut.processPackageQueue()

        assertTrue(result, "Should return true when a request is processed")
        assertContains(output.out, "Error during claiming an indexing request")

        // Verify the failed_attempts count is incremented
        val failedAttempts = jdbcTemplate.queryForObject(
            "SELECT failed_attempts FROM package_index_request WHERE id = ${packageIndexRequest.idNotNull}",
            Int::class.java
        )
        assertEquals(1, failedAttempts, "Failed attempts should be incremented")
        assertContains(output.out, "Mocked buildFromMarkdown exception")
    }

    @Test
    fun `should save SUCCESS report when indexing a user-originated request`() {
        val issue = saveUserOriginatedRequest(failedAttempts = 0)

        val pom = mock<MavenPom>()
        whenever(pom.groupId).thenReturn("com.example")
        whenever(pom.artifactId).thenReturn("test-artifact")
        whenever(pom.version).thenReturn("1.0.0")
        val kotlinToolingMetadata = mock<GradleMetadata>()
        whenever(kotlinToolingMetadata.variants).thenReturn(listOf(Variant(mapOf("org.jetbrains.kotlin.platform.type" to "js"))))
        val kotlinToolingMetadataDelegate = KotlinToolingMetadataDelegateStubImpl(kotlinToolingMetadata)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any()))
            .thenReturn(PomWithReleaseDate(pom, Instant.now()))
        whenever(mavenStaticDataProvider.getKotlinToolingMetadata(any()))
            .thenReturn(kotlinToolingMetadataDelegate)

        uut.processPackageQueue()

        val reports = userRequestReportRepository.findAll().toList()
        assertEquals(1, reports.size, "Exactly one report expected")
        val report = reports.first()
        assertEquals(UserRequestIndexingStatus.SUCCESS, report.indexingStatus)
        assertEquals("com.example", report.groupId)
        assertEquals("test-artifact", report.artifactId)
        assertEquals("1.0.0", report.version)

        val linkedIssueId = jdbcTemplate.queryForObject(
            "SELECT user_request_issue_id::text FROM user_request_report",
            String::class.java
        )
        assertEquals(issue.id.toString(), linkedIssueId, "Report should link to the originating issue")
    }

    @Test
    fun `should save FAILURE report when a user-originated request fails terminally`() {
        val issue = saveUserOriginatedRequest(failedAttempts = indexingRetryConfiguration.maxAttempts - 1)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any()))
            .thenThrow(RuntimeException("Mocked getPom exception"))

        uut.processPackageQueue()

        val reports = userRequestReportRepository.findAll().toList()
        assertEquals(1, reports.size, "Exactly one report expected")
        val report = reports.first()
        assertEquals(UserRequestIndexingStatus.FAILURE, report.indexingStatus)
        assertEquals("com.example", report.groupId)
        assertEquals("test-artifact", report.artifactId)
        assertEquals("1.0.0", report.version)
        assertEquals("Mocked getPom exception", report.statusDetails)

        val linkedIssueId = jdbcTemplate.queryForObject(
            "SELECT user_request_issue_id::text FROM user_request_report",
            String::class.java
        )
        assertEquals(issue.id.toString(), linkedIssueId, "Report should link to the originating issue")
    }

    @Test
    fun `should not save report when a user-originated request fails but can be retried`() {
        saveUserOriginatedRequest(failedAttempts = 0)
        whenever(mavenStaticDataProvider.getPomWithReleaseDate(any()))
            .thenThrow(RuntimeException("Mocked getPom exception"))

        uut.processPackageQueue()

        assertTrue(
            userRequestReportRepository.findAll().toList().isEmpty(),
            "No report should be saved until retries are exhausted"
        )
    }

    private fun saveUserOriginatedRequest(failedAttempts: Int): UserRequestIssueEntity {
        val issue = userRequestIssueRepository.save(
            UserRequestIssueEntity(
                githubIssueNumber = 7,
                groupId = "com.example",
                artifactId = "test-artifact",
                version = "1.0.0",
            )
        )
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "com.example",
                artifactId = "test-artifact",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.CENTRAL_SONATYPE,
                failedAttempts = failedAttempts,
                userRequestIssue = issue,
            )
        )
        return issue
    }
}
