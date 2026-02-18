package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.readme.ReadmeContentBuilder
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.impl.ReadmeMinimizationProcessor
import io.klibs.core.readme.service.ReadmeServiceDispatcher
import io.klibs.integration.ai.ProjectTagsGenerator
import io.klibs.integration.github.GitHubIntegration
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class ProjectIndexingServiceAddAiTagsTest {

    private val readmeServiceDispatcher: ReadmeServiceDispatcher = mock()
    private val projectDescriptionGenerator: io.klibs.integration.ai.ProjectDescriptionGenerator = mock()
    private val projectRepository: ProjectRepository = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val scmOwnerRepository: io.klibs.core.owner.ScmOwnerRepository = mock()
    private val projectTagsGenerator: ProjectTagsGenerator = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val gitHubIntegration: GitHubIntegration = mock()
    private val readmeContentBuilder: ReadmeContentBuilder = mock()
    private val readmeMinimizer: ReadmeMinimizationProcessor = mock()
    private val descriptionBackoffProvider: BackoffProvider = BackoffProvider("descriptionBackoff", mock())
    private val tagsBackoffProvider: BackoffProvider = BackoffProvider("descriptionBackoff", mock())

    private fun uut() = ProjectIndexingService(
            readmeServiceDispatcher = readmeServiceDispatcher,
            projectDescriptionGenerator = projectDescriptionGenerator,
            projectRepository = projectRepository,
            scmRepositoryRepository = scmRepositoryRepository,
            scmOwnerRepository = scmOwnerRepository,
            projectTagsGenerator = projectTagsGenerator,
            projectTagRepository = projectTagRepository,
            gitHubIntegration = gitHubIntegration,
            readmeContentBuilder = readmeContentBuilder,
            androidxReadmeProvider = mock(),
            readmeMinimizer = readmeMinimizer,
            descriptionBackoffProvider = descriptionBackoffProvider,
            tagsBackoffProvider = tagsBackoffProvider,
        )

    @Test
    fun `addAiTags should generate tags and save them with AI origin`() {
        val projectId = 101
        val scmRepoId = 202
        val readme = "# Awesome lib\nSome README content"
        val project = ProjectEntity(
            id = projectId,
            scmRepoId = scmRepoId,
            ownerId = 1,
            name = "test-repo",
            description = "Project long description",
            minimizedReadme = readme,
            latestVersion = "1.0.0",
            latestVersionTs = Instant.parse("2024-01-01T00:00:00Z")
        )
        whenever(projectRepository.findWithoutTags()).thenReturn(project)

        val repo = ScmRepositoryEntity(
            id = scmRepoId,
            nativeId = 9999,
            name = "awesome-lib",
            description = "GitHub repo description",
            defaultBranch = "main",
            createdTs = Instant.parse("2020-01-01T00:00:00Z"),
            ownerId = 1,
            ownerType = io.klibs.core.owner.ScmOwnerType.AUTHOR,
            ownerLogin = "octocat",
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 42,
            openIssues = 0,
            lastActivityTs = Instant.parse("2024-06-01T00:00:00Z"),
            updatedAtTs = Instant.parse("2024-06-01T00:00:00Z")
        )
        whenever(scmRepositoryRepository.findById(scmRepoId)).thenReturn(repo)
        whenever(readmeServiceDispatcher.readReadmeMd(
            ReadmeServiceDispatcher.ProjectInfo(projectId, scmRepoId, "test-repo", "octocat")
        )).thenReturn(readme)

        val generatedTags = listOf("kotlin", "testing", "http-client")
        whenever(
            projectTagsGenerator.generateTagsForProject(
                eq(project.name),
                eq(project.description ?: ""),
                eq(repo.description ?: ""),
                eq(readme)
            )
        ).thenReturn(generatedTags)

        whenever(projectTagRepository.saveAll(any<Iterable<TagEntity>>())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[0] as Iterable<TagEntity>).toList()
        }

        uut().addAiTags()

        val captor = argumentCaptor<Iterable<TagEntity>>()
        verify(projectTagRepository).saveAll(captor.capture())
        val saved = captor.firstValue.toList()

        assert(saved.size == generatedTags.size)
        saved.forEachIndexed { idx, tagEntity ->
            assert(tagEntity.projectId == projectId)
            assert(tagEntity.value == generatedTags[idx])
            assert(tagEntity.origin == TagOrigin.AI)
        }
    }

    @Test
    fun `addAiTags should do nothing when there is no project without tags`() {
        whenever(projectRepository.findWithoutTags()).thenReturn(null)

        uut().addAiTags()

        verify(scmRepositoryRepository, never()).findById(any<Int>())
        verify(readmeServiceDispatcher, never()).readReadmeMd(any())
        verify(projectTagsGenerator, never()).generateTagsForProject(any<String>(), any<String>(), any<String>(), any<String>())
        verify(projectTagRepository, never()).saveAll(any<Iterable<TagEntity>>())
    }

    @Test
    fun `addAiTags should backoff after failure and skip the same project on next run`() {
        val projectId = 11
        val scmRepoId = 22
        val project = ProjectEntity(
            id = projectId,
            scmRepoId = scmRepoId,
            ownerId = 1,
            name = "test-repo",
            description = "Desc",
            minimizedReadme = null,
            latestVersion = "1.0.0",
            latestVersionTs = Instant.parse("2024-01-01T00:00:00Z")
        )

        // Always return the same project (so second run hits backoff and exits)
        whenever(projectRepository.findWithoutTags()).thenReturn(project)

        val repo = ScmRepositoryEntity(
            id = scmRepoId,
            nativeId = 1001,
            name = "proj-one",
            description = "Repo desc",
            defaultBranch = "main",
            createdTs = Instant.parse("2020-01-01T00:00:00Z"),
            ownerId = 1,
            ownerType = io.klibs.core.owner.ScmOwnerType.AUTHOR,
            ownerLogin = "owner",
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = Instant.parse("2024-06-01T00:00:00Z"),
            updatedAtTs = Instant.parse("2024-06-01T00:00:00Z")
        )
        whenever(scmRepositoryRepository.findById(scmRepoId)).thenReturn(repo)
        whenever(readmeServiceDispatcher.readReadmeMd(
            ReadmeServiceDispatcher.ProjectInfo(projectId, scmRepoId, "test-repo", "owner")
        )).thenReturn("# Test README")

        // Force a failure during tag generation
        whenever(
            projectTagsGenerator.generateTagsForProject(any(), any(), any(), any())
        ).thenThrow(RuntimeException("AI tags generation failure"))

        val service = uut()

        // First run -> failure -> backoff recorded
        service.addAiTags()

        // Second run -> same project selected but is backed off -> should skip without calling generator again
        service.addAiTags()

        // Generator should be invoked only once (first run). Second run should skip early.
        verify(projectTagsGenerator).generateTagsForProject(any(), any(), any(), any())

        // No tags should be saved at all due to failure and then skip
        verify(projectTagRepository, never()).saveAll(any<Iterable<TagEntity>>())
    }
}
