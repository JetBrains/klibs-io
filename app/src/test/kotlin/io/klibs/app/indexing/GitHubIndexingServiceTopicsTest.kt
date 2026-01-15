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
import org.mockito.kotlin.*
import java.time.Instant

class GitHubIndexingServiceTopicsTest {

    private val gitHubIntegration: GitHubIntegration = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val scmOwnerRepository: io.klibs.core.owner.ScmOwnerRepository = mock()
    private val readmeService: ReadmeService = mock()
    private val projectRepository: ProjectRepository = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val readmeProcessors: List<ReadmeProcessor> = emptyList()
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository = mock()
    private val ownerBackoffProvider: BackoffProvider = mock()

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

        whenever(gitHubIntegration.getRepository(ghNativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getLicense(ghNativeId)).thenReturn(null)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(eq(ghNativeId), any()))
            .thenReturn(ReadmeFetchResult.NotFound)

        whenever(scmRepositoryRepository.findByName(ownerLogin, repoName)).thenReturn(existingRepo)
        whenever(scmRepositoryRepository.update(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ScmRepositoryEntity>(0)
            arg.copy(id = repoId)
        }

        whenever(projectRepository.findByScmRepoId(repoId)).thenReturn(project)
        whenever(gitHubIntegration.getRepositoryTopics(ghNativeId)).thenReturn(topicsFromGh)
        whenever(projectTagRepository.findAllByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB)).thenReturn(emptyList())

        whenever(allowedProjectTagsRepository.findCanonicalNameByValue(any<String>())).thenReturn(null)
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("kotlin")).thenReturn("kotlin")
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("spring")).thenReturn("spring")
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("web")).thenReturn("web")
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("compose-ui")).thenReturn("compose-ui")
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("flow")).thenReturn("kotlin-flow")

        val persisted = uut().updateRepo(existingRepo)

        assertEquals(repoId, persisted.id)
        verify(projectTagRepository).deleteByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB)

        val captor = argumentCaptor<Iterable<TagEntity>>()
        verify(projectTagRepository).saveAll(captor.capture())
        val savedTags = captor.firstValue.map { it.value }.sorted()
        assertEquals(expectedNormalized.sorted(), savedTags)

        captor.firstValue.forEach { tagEntity ->
            assertEquals(TagOrigin.GITHUB, tagEntity.origin)
            assertEquals(project.idNotNull, tagEntity.projectId)
        }

        verify(gitHubIntegration).getRepositoryTopics(ghNativeId)
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

        whenever(gitHubIntegration.getRepository(ghNativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getLicense(ghNativeId)).thenReturn(null)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(eq(ghNativeId), any()))
            .thenReturn(ReadmeFetchResult.NotFound)

        whenever(scmRepositoryRepository.findByName(ownerLogin, repoName)).thenReturn(existingRepo)
        whenever(scmRepositoryRepository.update(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ScmRepositoryEntity>(0)
            arg.copy(id = repoId)
        }

        whenever(projectRepository.findByScmRepoId(repoId)).thenReturn(project)
        whenever(gitHubIntegration.getRepositoryTopics(ghNativeId)).thenReturn(listOf("kotlin"))
        whenever(projectTagRepository.findAllByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB)).thenReturn(existingUserTags)

        whenever(allowedProjectTagsRepository.findCanonicalNameByValue(any<String>())).thenReturn(null)
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("kotlin")).thenReturn("kotlin")

        val persisted = uut().updateRepo(existingRepo)

        assertEquals(repoId, persisted.id)
        verify(projectTagRepository).deleteByProjectIdAndOrigin(project.idNotNull, TagOrigin.GITHUB)
        verify(projectTagRepository, never()).saveAll(any<Iterable<TagEntity>>())
        verify(gitHubIntegration).getRepositoryTopics(ghNativeId)
    }
}
