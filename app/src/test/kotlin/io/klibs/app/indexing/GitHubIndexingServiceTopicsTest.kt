package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.ProjectService
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.readme.ReadmeContentBuilder
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.ReadmeFetchResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class GitHubIndexingServiceTopicsTest {

    private val gitHubIntegration: GitHubIntegration = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val scmOwnerRepository: io.klibs.core.owner.ScmOwnerRepository = mock()
    private val readmeMetadataRepository: ReadmeMetadataRepository = mock()
    private val readmeService: ReadmeService = mock()
    private val projectRepository: ProjectRepository = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val readmeContentBuilder: ReadmeContentBuilder = mock()
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository = mock()
    private val ownerBackoffProvider: BackoffProvider = mock()
    private val projectService: ProjectService = mock()
    private val readmeReprocessPeriodDays: Long = 7

    private fun uut() = GitHubIndexingService(
        gitHubIntegration = gitHubIntegration,
        scmRepositoryRepository = scmRepositoryRepository,
        scmOwnerRepository = scmOwnerRepository,
        readmeMetadataRepository = readmeMetadataRepository,
        readmeService = readmeService,
        readmeContentBuilder = readmeContentBuilder,
        projectRepository = projectRepository,
        ownerBackoffProvider = ownerBackoffProvider,
        projectService = projectService,
        readmeReprocessPeriodDays = readmeReprocessPeriodDays
    )

    @Test
    fun `updateRepo updates GitHub topics for linked project`() {
        // Given: an existing repo and a linked project
        val repoId = 1
        val ghNativeId = 1234L
        val ownerLogin = "alice"
        val repoName = "demo"
        val existingRepo = ScmRepositoryEntity(
            id = repoId,
            nativeId = ghNativeId,
            name = repoName,
            description = "desc",
            defaultBranch = "main",
            createdTs = Instant.now().minusSeconds(3600),
            ownerId = 10,
            ownerType = ScmOwnerType.AUTHOR,
            ownerLogin = ownerLogin,
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            hasReadme = false,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = Instant.now().minusSeconds(1800),
            updatedAtTs = Instant.now().minusSeconds(300),
        )

        val ghRepo = GitHubRepository(
            nativeId = ghNativeId,
            name = repoName,
            createdAt = Instant.now().minusSeconds(7200),
            description = "new desc",
            defaultBranch = "main",
            owner = ownerLogin,
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            stars = 42,
            openIssues = 1,
            lastActivity = Instant.now(),
        )

        val project = ProjectEntity(
            id = 101,
            scmRepoId = repoId,
            ownerId = 10,
            name = "awesome-lib",
            description = null,
            minimizedReadme = null,
            latestVersion = "1.0.0",
            latestVersionTs = Instant.now().minusSeconds(100),
        )

        // Topics with mixed case, blanks, and duplicates
        val topicsFromGh = listOf("Kotlin", "kotlin", "  SPRING  ", "", "Web", "compose UI", "flow")

        whenever(gitHubIntegration.getRepository(ghNativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getLicense(ghNativeId)).thenReturn(null)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(eq(ghNativeId), any()))
            .thenReturn(ReadmeFetchResult.NotFound)

        whenever(scmRepositoryRepository.findByName(ownerLogin, repoName)).thenReturn(existingRepo)
        whenever(scmRepositoryRepository.update(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ScmRepositoryEntity>(0)
            arg.copy(id = repoId)
        }

        whenever(projectRepository.findByNameAndScmRepoId(any(), eq(repoId))).thenReturn(project)
        whenever(gitHubIntegration.getRepositoryTopics(ghNativeId)).thenReturn(topicsFromGh)
        whenever(projectTagRepository.findAllByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB)).thenReturn(emptyList())

        whenever(allowedProjectTagsRepository.existsById(any<String>())).thenReturn(false)
        whenever(allowedProjectTagsRepository.existsById("kotlin")).thenReturn(true)
        whenever(allowedProjectTagsRepository.existsById("spring")).thenReturn(true)
        whenever(allowedProjectTagsRepository.existsById("web")).thenReturn(true)
        whenever(allowedProjectTagsRepository.existsById("compose-ui")).thenReturn(true)
        whenever(allowedProjectTagsRepository.existsById("flow")).thenReturn(false)

        uut().updateRepo(existingRepo)

        verify(projectService).updateProjectTags(existingRepo.name, existingRepo.ownerLogin, topicsFromGh, TagOrigin.GITHUB)
    }
}
