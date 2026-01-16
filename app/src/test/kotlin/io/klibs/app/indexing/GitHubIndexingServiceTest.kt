package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.any
import io.mockk.every
import io.mockk.verify
import io.mockk.confirmVerified
import org.springframework.beans.factory.annotation.Autowired
import com.ninjasquad.springmockk.MockkBean
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull


@ExtendWith(OutputCaptureExtension::class)
class GitHubIndexingServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockkBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockkBean(name = "ownerBackoffProvider")
    private lateinit var ownerBackoffProvider: BackoffProvider

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

        uut.syncOwnerWithGitHub()

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
    }

    @Sql(value = ["classpath:sql/GitHubIndexingServiceTest/insert-owner-for-update.sql"])
    @Test
    fun `updateOwner should handle exception when GitHub user not found`(output: CapturedOutput) {

        every { gitHubIntegration.getUser(any()) } returns null

        uut.syncOwnerWithGitHub()

        verify(exactly = 1) { gitHubIntegration.getUser(any()) }
        assertContains(output.out, "Error while updating a GitHub owner")
    }

    @Test
    fun `updateOwner should do nothing when no owner for update exists`(output: CapturedOutput) {

        uut.syncOwnerWithGitHub()

        confirmVerified(gitHubIntegration)
        assert(!output.out.contains("Error while updating a GitHub owner"))
    }

    @Sql(value = ["classpath:sql/GitHubIndexingServiceTest/insert-owner-for-update.sql"])
    @Test
    fun `updateOwner should handle exception when GitHub integration fails`(output: CapturedOutput) {

        every { gitHubIntegration.getUser(any()) } throws RuntimeException("API error")

        uut.syncOwnerWithGitHub()

        verify(exactly = 1) { gitHubIntegration.getUser(any()) }
        assertContains(output.out, "Error while updating a GitHub owner")
    }


    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    @Test
    fun `updateRepository should update repository when found`() {
        val repositoryNativeId = 598863246L
        val ownerLogin = "k-libs"
        val updatedLicenseKey = "Updated license key"
        val updatedLicenseValue = "Updated license value"

        val repositoryBeforeTest = scmRepositoryRepository.findByNativeId(repositoryNativeId)
        assertNotNull(repositoryBeforeTest, "Repository entity should exist before test")

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
        every { gitHubIntegration.getLicense(repositoryNativeId) } returns (
            GitHubLicense(
                updatedLicenseKey,
                updatedLicenseValue
            )
        )
        every {
            gitHubIntegration.getReadmeWithModifiedSinceCheck(repositoryNativeId, repositoryBeforeTest.updatedAtTs)
        } returns ReadmeFetchResult.Content("Updated readme")
        every { gitHubIntegration.markdownToHtml("Updated readme", repositoryNativeId) } returns "<p>Updated readme</p>"
        every { gitHubIntegration.markdownRender("Updated readme", repositoryNativeId) } returns "Updated readme (rendered)"

        uut.updateRepo(repositoryBeforeTest)

        verify { gitHubIntegration.getRepository(repositoryNativeId) }

        val repositoryAfterTest = scmRepositoryRepository.findByNativeId(repositoryNativeId)
        assertNotNull(repositoryAfterTest, "Repository entity should exist after test")

        val expectedRepositoryAfterTest = repositoryBeforeTest.copy(
            name = ghRepo.name,
            description = ghRepo.description,
            defaultBranch = ghRepo.defaultBranch,
            ownerLogin = ghRepo.owner,
            homepage = ghRepo.homepage,
            hasGhPages = ghRepo.hasGhPages,
            hasIssues = ghRepo.hasIssues,
            hasWiki = ghRepo.hasWiki,
            stars = ghRepo.stars,
            updatedAtTs = repositoryAfterTest.updatedAtTs,
            lastActivityTs = repositoryAfterTest.lastActivityTs,
            licenseKey = updatedLicenseKey,
            licenseName = updatedLicenseValue,
            openIssues = ghRepo.openIssues,
            hasReadme = true,
            minimizedReadme = repositoryAfterTest.minimizedReadme,
        )
        assertEquals(expectedRepositoryAfterTest, repositoryAfterTest)

    }
}
