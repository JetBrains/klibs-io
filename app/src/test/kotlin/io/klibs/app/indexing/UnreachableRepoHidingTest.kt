package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.core.project.ProjectService
import io.klibs.core.project.enums.HideOrigin
import io.klibs.core.project.repository.ProjectHiddenRepository
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.ReadmeFetchResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Runs on the configured corpus-share limit. Every test seeds seven extra reachable repositories, so one
 * unreachable repository is a small share of the corpus and marking several of them trips the guard.
 */
class UnreachableRepoHidingTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @Autowired
    private lateinit var readmeMetadataRepository: ReadmeMetadataRepository

    @Autowired
    private lateinit var projectHiddenRepository: ProjectHiddenRepository

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockitoBean
    private lateinit var s3ReadmeService: S3ReadmeCRUDService

    @MockitoBean
    private lateinit var projectService: ProjectService

    @Test
    @Sql(
        scripts = [
            "classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql",
            "classpath:sql/UnreachableRepoHidingTest/insert-reachable-repositories.sql"
        ]
    )
    fun `unreachable for less than the threshold keeps the project visible`() {
        val repo = unreachableFor(Duration.ofDays(3))
        gitHubAnswering()
        repoNotFoundOnGitHub(repo)

        uut.updateRepo(repo)

        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
    }

    @Test
    @Sql(
        scripts = [
            "classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql",
            "classpath:sql/GitHubIndexingServiceTest/insert-second-project-for-repo.sql",
            "classpath:sql/UnreachableRepoHidingTest/insert-reachable-repositories.sql"
        ]
    )
    fun `unreachable past the threshold hides every project of the repository`() {
        val repo = unreachableFor(Duration.ofDays(8))
        gitHubAnswering()
        repoNotFoundOnGitHub(repo)

        uut.updateRepo(repo)

        listOf(PROJECT_ID, SECOND_PROJECT_ID).forEach { projectId ->
            val hidden = assertNotNull(
                projectHiddenRepository.findByProjectId(projectId),
                "projectId=$projectId is not hidden"
            )
            assertEquals(HideOrigin.AUTO, hidden.origin)
        }
    }

    @Test
    @Sql(
        scripts = [
            "classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql",
            "classpath:sql/UnreachableRepoHidingTest/insert-reachable-repositories.sql"
        ]
    )
    fun `repository reachable again un-hides an automatically hidden project`() {
        val repo = unreachableFor(Duration.ofDays(8))
        gitHubAnswering()
        repoNotFoundOnGitHub(repo)
        uut.updateRepo(repo)
        assertNotNull(projectHiddenRepository.findByProjectId(PROJECT_ID))

        val stillUnreachable = requireNotNull(scmRepositoryRepository.findById(repo.idNotNull))
        repoFoundOnGitHub(stillUnreachable)

        uut.updateRepo(stillUnreachable)

        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
    }

    @Test
    @Sql(
        scripts = [
            "classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql",
            "classpath:sql/UnreachableRepoHidingTest/insert-reachable-repositories.sql"
        ]
    )
    fun `a manual hide survives a successful repository fetch`() {
        val repo = requireNotNull(scmRepositoryRepository.findByNativeId(REPO_NATIVE_ID))
        projectHiddenRepository.hide(PROJECT_ID, Instant.now(), HideOrigin.MANUAL, "spam")
        repoFoundOnGitHub(repo)

        uut.updateRepo(repo)

        val hidden = assertNotNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
        assertEquals(HideOrigin.MANUAL, hidden.origin)
        assertEquals("spam", hidden.reason)
    }

    @Test
    @Sql(
        scripts = [
            "classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql",
            "classpath:sql/UnreachableRepoHidingTest/insert-reachable-repositories.sql"
        ]
    )
    fun `an unanswering GitHub API blocks hiding`() {
        val repo = unreachableFor(Duration.ofDays(8))
        whenever(gitHubIntegration.getRateLimitInfo()).thenThrow(RuntimeException("401 Bad credentials"))
        repoNotFoundOnGitHub(repo)

        uut.updateRepo(repo)

        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
    }

    @Test
    @Sql(
        scripts = [
            "classpath:sql/GitHubIndexingServiceTest/insert-repository-for-update.sql",
            "classpath:sql/UnreachableRepoHidingTest/insert-reachable-repositories.sql"
        ]
    )
    fun `too large a share of unreachable repositories blocks hiding`() {
        val repo = unreachableFor(Duration.ofDays(8))
        unreachableFillerRepositories(count = 3)
        gitHubAnswering()
        repoNotFoundOnGitHub(repo)

        uut.updateRepo(repo)

        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
    }

    private fun unreachableFor(duration: Duration): ScmRepositoryEntity {
        val repo = requireNotNull(scmRepositoryRepository.findByNativeId(REPO_NATIVE_ID))
        scmRepositoryRepository.markUnreachable(repo.idNotNull, Instant.now().minus(duration))
        return requireNotNull(scmRepositoryRepository.findById(repo.idNotNull))
    }

    private fun unreachableFillerRepositories(count: Int) {
        (1..count).forEach { n ->
            val filler = requireNotNull(scmRepositoryRepository.findByNativeId(FILLER_REPO_NATIVE_ID_BASE + n))
            scmRepositoryRepository.markUnreachable(filler.idNotNull, Instant.now())
        }
    }

    private fun gitHubAnswering() {
        whenever(gitHubIntegration.getRateLimitInfo()).thenReturn(rateLimit(limit = 5000))
    }

    private fun repoNotFoundOnGitHub(repo: ScmRepositoryEntity) {
        whenever(gitHubIntegration.getRepository(repo.nativeId)).thenReturn(null)
        whenever(gitHubIntegration.getRepository(repo.ownerLogin, repo.name)).thenReturn(null)
    }

    private fun repoFoundOnGitHub(repo: ScmRepositoryEntity) {
        val readmeMetadata = requireNotNull(readmeMetadataRepository.findByScmRepoId(repo.idNotNull))

        whenever(gitHubIntegration.getRepository(repo.nativeId)).thenReturn(
            GitHubRepository(
                nativeId = repo.nativeId,
                name = repo.name,
                createdAt = repo.createdTs,
                description = repo.description,
                defaultBranch = repo.defaultBranch,
                owner = repo.ownerLogin,
                homepage = repo.homepage,
                hasGhPages = repo.hasGhPages,
                hasIssues = repo.hasIssues,
                hasWiki = repo.hasWiki,
                archived = repo.archived,
                stars = repo.stars,
                openIssues = repo.openIssues,
                lastActivity = repo.lastActivityTs
            )
        )
        whenever(gitHubIntegration.getLicense(repo.nativeId))
            .thenReturn(GitHubLicense(repo.licenseKey!!, repo.licenseName!!))
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(repo.nativeId, readmeMetadata.lastSyncedAt))
            .thenReturn(ReadmeFetchResult.NotModified)
        whenever(gitHubIntegration.getRepositoryTopics(repo.nativeId)).thenReturn(emptyList())
    }

    private companion object {
        const val REPO_NATIVE_ID = 598863246L
        const val FILLER_REPO_NATIVE_ID_BASE = 700000L
        const val PROJECT_ID = 10001
        const val SECOND_PROJECT_ID = 10002

        fun rateLimit(limit: Int) = GitHubRateLimitInfo(
            limit = limit,
            remaining = limit,
            resetAt = Instant.now().plus(Duration.ofHours(1))
        )
    }
}
