package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeProcessor
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.ReadmeFetchResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.any
import io.mockk.eq
import java.time.Instant

class GitHubIndexingServiceTopicsTest {

    private val gitHubIntegration: GitHubIntegration = mockk()
    private val scmRepositoryRepository: ScmRepositoryRepository = mockk()
    private val scmOwnerRepository: io.klibs.core.owner.ScmOwnerRepository = mockk()
    private val readmeService: ReadmeService = mockk()
    private val projectRepository: ProjectRepository = mockk()
    private val projectTagRepository: ProjectTagRepository = mockk()
    private val readmeProcessors: List<ReadmeProcessor> = emptyList()
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository = mockk()
    private val ownerBackoffProvider: BackoffProvider = mockk()

    private fun uut() = GitHubIndexingService(
        gitHubIntegration = gitHubIntegration,
        scmRepositoryRepository = scmRepositoryRepository,
        scmOwnerRepository = scmOwnerRepository,
        readmeService = readmeService,
        readmeProcessors = readmeProcessors,
        projectRepository = projectRepository,
        projectTagRepository = projectTagRepository,
        allowedProjectTagsRepository = allowedProjectTagsRepository,
        ownerBackoffProvider = ownerBackoffProvider,
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
            minimizedReadme = null,
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
            description = null,
            latestVersion = "1.0.0",
            latestVersionTs = Instant.now().minusSeconds(100),
        )

        // Topics with mixed case, blanks, and duplicates
        val topicsFromGh = listOf("Kotlin", "kotlin", "  SPRING  ", "", "Web", "compose UI", "flow")
        val expectedNormalized = listOf("kotlin", "spring", "web", "compose-ui", "kotlin-flow")

        every { gitHubIntegration.getRepository(ghNativeId) } returns ghRepo
        every { gitHubIntegration.getLicense(ghNativeId) } returns null
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(eq(ghNativeId), any()) } returns ReadmeFetchResult.NotFound

        every { scmRepositoryRepository.findByName(ownerLogin, repoName) } returns existingRepo
        every { scmRepositoryRepository.update(any()) } answers { firstArg<ScmRepositoryEntity>().copy(id = repoId) }

        every { projectRepository.findByScmRepoId(repoId) } returns project
        every { gitHubIntegration.getRepositoryTopics(ghNativeId) } returns topicsFromGh
        every { projectTagRepository.findAllByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB) } returns emptyList()

        every { allowedProjectTagsRepository.findCanonicalNameByValue(any<String>()) } returns null
        every { allowedProjectTagsRepository.findCanonicalNameByValue("kotlin") } returns "kotlin"
        every { allowedProjectTagsRepository.findCanonicalNameByValue("spring") } returns "spring"
        every { allowedProjectTagsRepository.findCanonicalNameByValue("web") } returns "web"
        every { allowedProjectTagsRepository.findCanonicalNameByValue("compose-ui") } returns "compose-ui"
        every { allowedProjectTagsRepository.findCanonicalNameByValue("flow") } returns "kotlin-flow"

        val persisted = uut().updateRepo(existingRepo)

        assertEquals(repoId, persisted.id)
        verify { projectTagRepository.deleteByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB) }

        val captor: CapturingSlot<Iterable<TagEntity>> = slot()
        verify { projectTagRepository.saveAll(capture(captor)) }
        val savedTags = captor.captured.map { it.value }.sorted()
        assertEquals(expectedNormalized.sorted(), savedTags)

        captor.captured.forEach { tagEntity ->
            assertEquals(TagOrigin.GITHUB, tagEntity.origin)
            assertEquals(project.idNotNull, tagEntity.projectId)
        }

        verify { gitHubIntegration.getRepositoryTopics(ghNativeId) }
    }

    @Test
    fun `if USER-origin tag exists, same GITHUB-origin tag is ignored`() {
        // Given: an existing repo and a linked project
        val repoId = 2
        val ghNativeId = 2222L
        val ownerLogin = "bob"
        val repoName = "sample"
        val existingRepo = ScmRepositoryEntity(
            id = repoId,
            nativeId = ghNativeId,
            name = repoName,
            description = "desc",
            defaultBranch = "main",
            createdTs = Instant.now().minusSeconds(3600),
            ownerId = 11,
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
            minimizedReadme = null,
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
            stars = 1,
            openIssues = 0,
            lastActivity = Instant.now(),
        )

        val project = ProjectEntity(
            id = 202,
            scmRepoId = repoId,
            description = null,
            latestVersion = "0.1.0",
            latestVersionTs = Instant.now().minusSeconds(100),
        )

        val existingUserTags = listOf(TagEntity(project.idNotNull, TagOrigin.USER, "kotlin"))

        every { gitHubIntegration.getRepository(ghNativeId) } returns ghRepo
        every { gitHubIntegration.getLicense(ghNativeId) } returns null
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(eq(ghNativeId), any()) } returns ReadmeFetchResult.NotFound

        every { scmRepositoryRepository.findByName(ownerLogin, repoName) } returns existingRepo
        every { scmRepositoryRepository.update(any()) } answers { firstArg<ScmRepositoryEntity>().copy(id = repoId) }

        every { projectRepository.findByScmRepoId(repoId) } returns project
        every { gitHubIntegration.getRepositoryTopics(ghNativeId) } returns listOf("kotlin")
        every { projectTagRepository.findAllByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB) } returns existingUserTags

        every { allowedProjectTagsRepository.findCanonicalNameByValue(any<String>()) } returns null
        every { allowedProjectTagsRepository.findCanonicalNameByValue("kotlin") } returns "kotlin"

        val persisted = uut().updateRepo(existingRepo)

        assertEquals(repoId, persisted.id)
        verify { projectTagRepository.deleteByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB) }
        verify(exactly = 0) { projectTagRepository.saveAll(any()) }
        verify { gitHubIntegration.getRepositoryTopics(ghNativeId) }
    }
}
