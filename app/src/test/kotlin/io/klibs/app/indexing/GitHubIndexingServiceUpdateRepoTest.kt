package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.core.owner.ScmOwnerEntity
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import org.junit.jupiter.api.Test
import io.mockk.verify
import io.mockk.confirmVerified
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubIndexingServiceUpdateRepoTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockkBean
    private lateinit var gitHubIntegration: GitHubIntegration

    private val repoNativeId = 598863246L
    private val initialOwnerLogin = "k-libs"

    private lateinit var ghRepoBefore: GitHubRepository

    private lateinit var ghLicenseBefore: GitHubLicense

    private lateinit var ghOwnerBefore: GitHubUser

    private fun initVars(scmRepoBefore: ScmRepositoryEntity, ownerBefore: ScmOwnerEntity? = null) {
        ghRepoBefore =  GitHubRepository(
            nativeId = repoNativeId,
            name = scmRepoBefore.name,
            createdAt = scmRepoBefore.createdTs,
            description = scmRepoBefore.description,
            defaultBranch = scmRepoBefore.defaultBranch,
            owner = initialOwnerLogin,
            homepage = scmRepoBefore.homepage,
            hasGhPages = scmRepoBefore.hasGhPages,
            hasIssues = scmRepoBefore.hasIssues,
            hasWiki = scmRepoBefore.hasWiki,
            stars = scmRepoBefore.stars,
            openIssues = scmRepoBefore.openIssues,
            lastActivity = scmRepoBefore.lastActivityTs,
        )

        ghLicenseBefore = GitHubLicense(
            scmRepoBefore.licenseKey!!,
            scmRepoBefore.licenseName!!
        )

        ghOwnerBefore = GitHubUser(
            id = ownerBefore?.nativeId ?: 198,
            login = ownerBefore?.login ?: scmRepoBefore.ownerLogin,
            type = "Organization", // cannot copy it from owner entity, since io.klibs.app.indexing.GitHubIndexingService.getOwnerType
            name = ownerBefore?.name ?: "k-libs",
            company = ownerBefore?.company,
            blog = ownerBefore?.homepage,
            location = ownerBefore?.location,
            email = ownerBefore?.email,
            bio = ownerBefore?.description,
            twitterUsername = ownerBefore?.twitterHandle,
            followers = ownerBefore?.followers ?: 0
        )
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    fun `updateRepo - github repo not found updates timestamp only`() {
        val before = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        initVars(before)

        every { gitHubIntegration.getRepository(repoNativeId) } returns null
        every { gitHubIntegration.getRepository(before.ownerLogin, before.name) } returns null

        uut.updateRepo(before)

        verify { gitHubIntegration.getRepository(repoNativeId) }
        verify { gitHubIntegration.getRepository(before.ownerLogin, before.name) }
        confirmVerified(gitHubIntegration)

        val after = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))

        assertTrue(after.updatedAtTs.isAfter(before.updatedAtTs))
        val beforeUpdated = before.copy(updatedAtTs = after.updatedAtTs)
        assertEquals(beforeUpdated, after)
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    fun `updateRepo - owner unchanged keeps same owner id`() {
        val before = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        initVars(before)

        val ghRepo = ghRepoBefore.copy(
            description = "Updated repo description"
        )

        every { gitHubIntegration.getRepository(repoNativeId) } returns ghRepo
        every { gitHubIntegration.getLicense(repoNativeId) } returns ghLicenseBefore

        every {
            gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, before.updatedAtTs)
        } returns ReadmeFetchResult.Content("Updated readme")
        every { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) } returns "<p>Updated readme</p>"
        every { gitHubIntegration.markdownRender("Updated readme", repoNativeId) } returns "Updated readme (rendered)"

        uut.updateRepo(before)

        verify { gitHubIntegration.getRepository(repoNativeId) }
        verify { gitHubIntegration.getLicense(repoNativeId) }
        verify { gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, before.updatedAtTs) }
        verify { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) }
        verify { gitHubIntegration.markdownRender("Updated readme", repoNativeId) }
        confirmVerified(gitHubIntegration)

        val after = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))

        assertTrue(after.updatedAtTs.isAfter(before.updatedAtTs))
        val beforeUpdated = before.copy(
            description = ghRepo.description,
            updatedAtTs = after.updatedAtTs,
            minimizedReadme = after.minimizedReadme,
        )
        assertEquals(beforeUpdated, after)
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    fun `updateRepo - owner changed with different nativeId relocates to new owner`() {
        val repoBefore = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        val ownerBefore = requireNotNull(scmOwnerRepository.findById(repoBefore.ownerId))
        initVars(repoBefore, ownerBefore)

        val newOwnerLogin = "new-org"
        val newOwnerNativeId = 9999999L

        val ghRepo = ghRepoBefore.copy(
            owner = newOwnerLogin,
        )

        val ghUser = ghOwnerBefore.copy(
            id = newOwnerNativeId,
            login = newOwnerLogin,
            name = newOwnerLogin,
        )

        every { gitHubIntegration.getRepository(repoNativeId) } returns ghRepo
        every { gitHubIntegration.getUser(newOwnerLogin) } returns ghUser
        every { gitHubIntegration.getLicense(repoNativeId) } returns ghLicenseBefore

        every {
            gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, repoBefore.updatedAtTs)
        } returns ReadmeFetchResult.Content("Updated readme")
        every { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) } returns "<p>Updated readme</p>"
        every { gitHubIntegration.markdownRender("Updated readme", repoNativeId) } returns "Updated readme (rendered)"

        uut.updateRepo(repoBefore)

        verify { gitHubIntegration.getRepository(repoNativeId) }
        verify { gitHubIntegration.getUser(newOwnerLogin) }
        verify { gitHubIntegration.getLicense(repoNativeId) }
        verify { gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, repoBefore.updatedAtTs) }
        verify { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) }
        verify { gitHubIntegration.markdownRender("Updated readme", repoNativeId) }

        confirmVerified(gitHubIntegration)

        val repoAfter = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))

        val repoAfterExpected = repoBefore.copy(
            ownerId = repoAfter.ownerId,
            ownerLogin = newOwnerLogin,
            updatedAtTs = repoAfter.updatedAtTs,
        )

        assertEquals(repoAfterExpected, repoAfter)

        val newOwner = requireNotNull(scmOwnerRepository.findById(repoAfter.ownerId))

        val newOwnerExpected = ownerBefore.copy(
            id = newOwner.id,
            nativeId = newOwnerNativeId,
            login = newOwnerLogin,
            name = newOwnerLogin,
            updatedAtTs = newOwner.updatedAtTs,
        )

        assertEquals(newOwnerExpected, newOwner)
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    fun `updateRepo - owner changed but same nativeId updates login and keeps owner id`() {
        val repoBefore = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        val ownerBefore = requireNotNull(scmOwnerRepository.findById(repoBefore.ownerId))
        initVars(repoBefore, ownerBefore)

        val renamedLogin = "k-libs-renamed"

        val ghRepo = ghRepoBefore.copy(
            owner = renamedLogin,
        )

        val ghUserSameNative = ghOwnerBefore.copy(
            login = renamedLogin,
        )

        every { gitHubIntegration.getRepository(repoNativeId) } returns ghRepo
        every { gitHubIntegration.getUser(renamedLogin) } returns ghUserSameNative
        every { gitHubIntegration.getLicense(repoNativeId) } returns ghLicenseBefore

        every {
            gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, repoBefore.updatedAtTs)
        } returns ReadmeFetchResult.Content("Updated readme")
        every { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) } returns "<p>Updated readme</p>"
        every { gitHubIntegration.markdownRender("Updated readme", repoNativeId) } returns "Updated readme (rendered)"

        uut.updateRepo(repoBefore)

        verify { gitHubIntegration.getRepository(repoNativeId) }
        verify { gitHubIntegration.getUser(renamedLogin) }
        verify { gitHubIntegration.getLicense(repoNativeId) }
        verify { gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, repoBefore.updatedAtTs) }
        verify { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) }
        verify { gitHubIntegration.markdownRender("Updated readme", repoNativeId) }

        confirmVerified(gitHubIntegration)

        val repoAfter = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        val repoAfterExpected = repoBefore.copy(
            ownerLogin = renamedLogin,
            updatedAtTs = repoAfter.updatedAtTs,
        )
        assertEquals(repoAfterExpected, repoAfter)

        val ownerAfter = requireNotNull(scmOwnerRepository.findById(repoBefore.ownerId))
        val ownerAfterExpected = ownerBefore.copy(
            login = renamedLogin,
            updatedAtTs = ownerAfter.updatedAtTs,
        )
        assertEquals(ownerAfterExpected, ownerAfter)
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql"])
    fun `updateRepo - repository renamed under same owner uses upsert to change name`() {
        val repoBefore = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        initVars(repoBefore)

        val newName = repoBefore.name + "-renamed"

        val ghRepo = ghRepoBefore.copy(
            name = newName,
        )

        every { gitHubIntegration.getRepository(repoNativeId) } returns ghRepo
        every { gitHubIntegration.getLicense(repoNativeId) } returns ghLicenseBefore

        every {
            gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, repoBefore.updatedAtTs)
        } returns ReadmeFetchResult.Content("Updated readme")
        every { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) } returns "<p>Updated readme</p>"
        every { gitHubIntegration.markdownRender("Updated readme", repoNativeId) } returns "Updated readme (rendered)"

        uut.updateRepo(repoBefore)

        verify { gitHubIntegration.getRepository(repoNativeId) }
        verify { gitHubIntegration.getLicense(repoNativeId) }
        verify { gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, repoBefore.updatedAtTs) }
        verify { gitHubIntegration.markdownToHtml("Updated readme", repoNativeId) }
        verify { gitHubIntegration.markdownRender("Updated readme", repoNativeId) }

        confirmVerified(gitHubIntegration)

        val after = requireNotNull(scmRepositoryRepository.findByNativeId(repoNativeId))
        val expectedAfter = repoBefore.copy(
            name = newName,
            updatedAtTs = after.updatedAtTs,
        )
        assertEquals(expectedAfter, after)
    }
}
