package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.repository.ScmOwnerSchedulingRepository
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.project.ProjectService
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.data.repository.findByIdOrNull
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@ExtendWith(OutputCaptureExtension::class)
class GitHubIndexingServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @Autowired
    private lateinit var readmeMetadataRepository: ReadmeMetadataRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @MockkBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockkBean(relaxUnitFun = true)
    private lateinit var s3ReadmeService: S3ReadmeCRUDService

    @MockkBean
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var ownerUpdatingService: GitHubOwnerUpdatingService

    @Autowired
    private lateinit var ownerSchedulingRepository: ScmOwnerSchedulingRepository

    @Test
    @Sql(value = ["classpath:sql/GitHubIndexingServiceTest/insert-owner-for-update.sql"])
    fun `should update owner when owner for update exists`(output: CapturedOutput) {
        val login = "voize-gmbh"

        val ownerEntityBeforeTest = scmOwnerRepository.findByLogin(login)
        assertNotNull(ownerEntityBeforeTest, "Owner entity should exist before test")

        val githubUser = GitHubUser(
            id = 62517686L,
            login = login,
            type = "User",
            name = "Test User Updated",
            company = "New Company",
            blog = "https://new-blog.com",
            location = "New Location",
            email = "new@example.com",
            bio = "New bio",
            twitterUsername = "newtwitter",
            followers = 20
        )

        every { gitHubIntegration.getUser(login) } returns githubUser

        ownerUpdatingService.syncOwnerWithGitHub()

        val updatedOwnerEntity = scmOwnerRepository.findByLogin(login)
        assertNotNull(updatedOwnerEntity, "Owner entity should exist after sync method call")
        assertNotEquals(
            ownerEntityBeforeTest.updatedAtTs,
            updatedOwnerEntity.updatedAtTs,
            "Owner's updatedAtTs should have been updated"
        )
        val expectedUpdatedOwnerEntity = ownerEntityBeforeTest.copy(
            name = githubUser.name,
            type = ScmOwnerType.AUTHOR,
            description = githubUser.bio,
            location = githubUser.location,
            followers = githubUser.followers,
            company = githubUser.company,
            homepage = githubUser.blog,
            twitterHandle = githubUser.twitterUsername,
            email = githubUser.email,
            updatedAtTs = updatedOwnerEntity.updatedAtTs
        )

        assertEquals(expectedUpdatedOwnerEntity, updatedOwnerEntity)
        assert(!output.out.contains("Error while updating a GitHub owner"))
        assertNull(
            ownerSchedulingRepository.findByIdOrNull(updatedOwnerEntity.idNotNull),
            "a successful sync should leave the owner eligible"
        )
    }

    @Sql(value = ["classpath:sql/GitHubIndexingServiceTest/insert-owner-for-update.sql"])
    @Test
    fun `an owner that resolves by neither login nor native id is deferred as deleted`(output: CapturedOutput) {

        every { gitHubIntegration.getUser(any<String>()) } returns null
        every { gitHubIntegration.getUser(any<Long>()) } returns null

        ownerUpdatingService.syncOwnerWithGitHub()

        val deferred = assertNotNull(
            ownerSchedulingRepository.findByIdOrNull(ownerId("voize-gmbh")),
            "a deleted owner should be deferred so it stops being re-selected"
        )
        assertContains(deferred.reason, "ScmOwnerDeletedException")
        assert(!output.out.contains("Error while updating a GitHub owner")) {
            "a deleted account is expected, so it should not reach the ERROR stream"
        }
    }

    @Test
    fun `updateOwner should do nothing when no owner for update exists`(output: CapturedOutput) {

        ownerUpdatingService.syncOwnerWithGitHub()

        confirmVerified(gitHubIntegration)
        assert(!output.out.contains("Error while updating a GitHub owner"))
    }

    @Sql(value = ["classpath:sql/GitHubIndexingServiceTest/insert-owner-for-update.sql"])
    @Test
    fun `updateOwner should handle exception when GitHub integration fails`(output: CapturedOutput) {

        every { gitHubIntegration.getUser(any<String>()) } throws RuntimeException("API error")

        ownerUpdatingService.syncOwnerWithGitHub()

        verify(exactly = 1) { gitHubIntegration.getUser(any<String>()) }
        assertContains(output.out, "Error while updating a GitHub owner")
    }


    private fun ownerId(login: String): Int =
        assertNotNull(scmOwnerRepository.findByLogin(login), "seeded owner $login should exist").idNotNull

    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    @Test
    fun `updateRepository should update repository when found`() {
        val repositoryNativeId = 598863246L
        val ownerLogin = "k-libs"
        val updatedLicenseKey = "Updated license key"
        val updatedLicenseValue = "Updated license value"

        val repositoryBeforeTest = scmRepositoryRepository.findByNativeId(repositoryNativeId)
        assertNotNull(repositoryBeforeTest, "Repository entity should exist before test")

        val readmeMetadataBeforeTest = readmeMetadataRepository.findByScmRepoId(repositoryBeforeTest.idNotNull)
        assertNotNull(readmeMetadataBeforeTest, "Readme metadata entity should exist before test")

        val fixedTime = Instant.now()

        val ghRepo = GitHubRepository(
            nativeId = repositoryNativeId,
            name = "updated-repo",
            createdAt = fixedTime.minusSeconds(7200),
            description = "Updated repository",
            defaultBranch = "updated main",
            owner = ownerLogin,
            homepage = "https://updated-example.com",
            hasGhPages = true,
            hasIssues = true,
            hasWiki = true,
            archived = false,
            stars = 100,
            openIssues = 5,
            lastActivity = fixedTime
        )

        val gitHubUser = GitHubUser(
            id = 198,
            "k-libs",
            "User",
            "k-libs",
            "organization",
            null,
            null,
            null,
            null,
            null,
            0
        )

        every { gitHubIntegration.getRepository(repositoryNativeId) } returns ghRepo
        every { gitHubIntegration.getUser(ownerLogin) } returns gitHubUser
        every { gitHubIntegration.getLicense(repositoryNativeId) } returns
            GitHubLicense(
                updatedLicenseKey,
                updatedLicenseValue
            )
        every {
            gitHubIntegration.getReadmeWithModifiedSinceCheck(repositoryNativeId, readmeMetadataBeforeTest.lastSyncedAt)
        } returns ReadmeFetchResult.Content("Updated readme")
        every { gitHubIntegration.markdownToHtml("Updated readme", repositoryNativeId) } returns "<p>Updated readme</p>"
        every { gitHubIntegration.markdownRender("Updated readme", repositoryNativeId) } returns "Updated readme (rendered)"
        every { gitHubIntegration.getRepositoryTopics(repositoryNativeId) } returns emptyList()

        uut.updateRepo(repositoryBeforeTest)

        verify(exactly = 1) { gitHubIntegration.getRepository(repositoryNativeId) }

        val repositoryAfterTest = scmRepositoryRepository.findByNativeId(repositoryNativeId)
        assertNotNull(repositoryAfterTest, "Repository entity should exist after test")

        val projectAfterTest = projectRepository.findByScmRepoId(repositoryAfterTest.idNotNull)
        assertNotNull(projectAfterTest, "Project entity should exist after test")
        assertEquals("Updated readme", projectAfterTest.minimizedReadme)

        val expectedRepositoryAfterTest = repositoryBeforeTest.copy(
            name = ghRepo.name,
            description = ghRepo.description,
            defaultBranch = ghRepo.defaultBranch,
            ownerLogin = ghRepo.owner,
            homepage = ghRepo.homepage,
            hasGhPages = ghRepo.hasGhPages,
            hasIssues = ghRepo.hasIssues,
            hasWiki = ghRepo.hasWiki,
            archived = ghRepo.archived,
            stars = ghRepo.stars,
            updatedAtTs = repositoryAfterTest.updatedAtTs,
            lastActivityTs = repositoryAfterTest.lastActivityTs,
            licenseKey = updatedLicenseKey,
            licenseName = updatedLicenseValue,
            openIssues = ghRepo.openIssues,
            hasReadme = true,
        )
        assertEquals(expectedRepositoryAfterTest, repositoryAfterTest)

    }
}
