package io.klibs.app.indexing

import io.klibs.core.owner.ScmOwnerRepository
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

class GitHubIndexingServiceTopicsTest {

    private val gitHubIntegration = mockk<GitHubIntegration>()
    private val scmRepositoryRepository = mockk<ScmRepositoryRepository>()
    private val scmOwnerRepository = mockk<ScmOwnerRepository>()
    private val readmeMetadataRepository = mockk<ReadmeMetadataRepository>()
    private val readmeService = mockk<ReadmeService>()
    private val projectRepository = mockk<ProjectRepository>()
    private val projectTagRepository = mockk<ProjectTagRepository>()
    private val readmeContentBuilder = mockk<ReadmeContentBuilder>()
    private val allowedProjectTagsRepository = mockk<AllowedProjectTagsRepository>()
    private val projectService = mockk<ProjectService>(relaxed = true)
    private val unreachableRepoHidingService = mockk<UnreachableRepoHidingService>()
    private val readmeReprocessPeriodDays: Long = 7

    private fun uut() = GitHubIndexingService(
        gitHubIntegration = gitHubIntegration,
        scmRepositoryRepository = scmRepositoryRepository,
        scmOwnerRepository = scmOwnerRepository,
        readmeMetadataRepository = readmeMetadataRepository,
        readmeService = readmeService,
        readmeContentBuilder = readmeContentBuilder,
        projectRepository = projectRepository,
        projectService = projectService,
        unreachableRepoHidingService = unreachableRepoHidingService,
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
            archived = false,
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
            archived = false,
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

        every { readmeMetadataRepository.findByScmRepoId(repoId) } returns null
        every { gitHubIntegration.getRepository(ghNativeId) } returns ghRepo
        every { gitHubIntegration.getLicense(ghNativeId) } returns null
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(ghNativeId, any()) } returns ReadmeFetchResult.NotFound

        every { scmRepositoryRepository.findByName(ownerLogin, repoName) } returns existingRepo
        every { scmRepositoryRepository.update(any()) } answers {
            firstArg<ScmRepositoryEntity>().copy(id = repoId)
        }

        every { projectRepository.findByNameAndScmRepoId(any(), repoId) } returns project
        every { gitHubIntegration.getRepositoryTopics(ghNativeId) } returns topicsFromGh
        every { projectTagRepository.findAllByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB) } returns emptyList()

        every { allowedProjectTagsRepository.existsById(any<String>()) } returns false
        every { allowedProjectTagsRepository.existsById("kotlin") } returns true
        every { allowedProjectTagsRepository.existsById("spring") } returns true
        every { allowedProjectTagsRepository.existsById("web") } returns true
        every { allowedProjectTagsRepository.existsById("compose-ui") } returns true
        every { allowedProjectTagsRepository.existsById("flow") } returns false

        uut().updateRepo(existingRepo)

        verify(exactly = 1) { projectService.updateProjectTags(existingRepo.name, existingRepo.ownerLogin, topicsFromGh, TagOrigin.GITHUB) }
    }
}
