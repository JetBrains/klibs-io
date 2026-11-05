package io.klibs.app.indexing

import io.klibs.app.service.TagsGenerationService
import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.readme.ReadmeContentBuilder
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.github.GitHubIntegration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

class ProjectIndexingServiceAddAiTagsTest {

    private val readmeService = mockk<ReadmeService>()
    private val projectDescriptionGenerator = mockk<ProjectDescriptionGenerator>()
    private val projectRepository = mockk<ProjectRepository>()
    private val scmRepositoryRepository = mockk<ScmRepositoryRepository>()
    private val scmOwnerRepository = mockk<ScmOwnerRepository>()
    private val tagsGenerator = mockk<TagsGenerationService>()
    private val projectTagRepository = mockk<ProjectTagRepository>()
    private val gitHubIntegration = mockk<GitHubIntegration>()
    private val readmeContentBuilder = mockk<ReadmeContentBuilder>()
    private val descriptionBackoffProvider = BackoffProvider("descriptionBackoff", mockk(relaxed = true))
    private val tagsBackoffProvider = BackoffProvider("descriptionBackoff", mockk(relaxed = true))

    private fun uut() = ProjectIndexingService(
        readmeService = readmeService,
        projectDescriptionGenerator = projectDescriptionGenerator,
        projectRepository = projectRepository,
        scmRepositoryRepository = scmRepositoryRepository,
        scmOwnerRepository = scmOwnerRepository,
        tagsGenerationService = tagsGenerator,
        projectTagRepository = projectTagRepository,
        gitHubIntegration = gitHubIntegration,
        readmeContentBuilder = readmeContentBuilder,
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
        every { projectRepository.findWithoutTags() } returns project

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
            archived = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 42,
            openIssues = 0,
            lastActivityTs = Instant.parse("2024-06-01T00:00:00Z"),
            updatedAtTs = Instant.parse("2024-06-01T00:00:00Z")
        )
        every { scmRepositoryRepository.findById(scmRepoId) } returns repo
        every {
            readmeService.readReadmeMd(
                ReadmeService.ProjectInfo(projectId, scmRepoId, "test-repo", "octocat")
            )
        } returns readme

        val generatedTags = listOf("kotlin", "testing", "http-client")
        every {
            tagsGenerator.generateTagsForProject(
                project.name,
                project.description ?: "",
                repo.description ?: "",
                readme
            )
        } returns generatedTags

        every { projectTagRepository.saveAll(any<Iterable<TagEntity>>()) } answers {
            firstArg<Iterable<TagEntity>>().toList()
        }

        uut().addAiTags()

        val captor = mutableListOf<Iterable<TagEntity>>()
        verify(exactly = 1) { projectTagRepository.saveAll(capture(captor)) }
        val saved = captor.first().toList()

        assert(saved.size == generatedTags.size)
        saved.forEachIndexed { idx, tagEntity ->
            assert(tagEntity.projectId == projectId)
            assert(tagEntity.value == generatedTags[idx])
            assert(tagEntity.origin == TagOrigin.AI)
        }
    }

    @Test
    fun `addAiTags should do nothing when there is no project without tags`() {
        every { projectRepository.findWithoutTags() } returns null

        uut().addAiTags()

        verify(exactly = 0) { scmRepositoryRepository.findById(any<Int>()) }
        verify(exactly = 0) { readmeService.readReadmeMd(any()) }
        verify(exactly = 0) { tagsGenerator.generateTagsForProject(any(), any(), any(), any()) }
        verify(exactly = 0) { projectTagRepository.saveAll(any<Iterable<TagEntity>>()) }
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
        every { projectRepository.findWithoutTags() } returns project

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
            archived = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = Instant.parse("2024-06-01T00:00:00Z"),
            updatedAtTs = Instant.parse("2024-06-01T00:00:00Z")
        )
        every { scmRepositoryRepository.findById(scmRepoId) } returns repo
        every {
            readmeService.readReadmeMd(
                ReadmeService.ProjectInfo(projectId, scmRepoId, "test-repo", "owner")
            )
        } returns "# Test README"

        // Force a failure during tag generation
        every {
            tagsGenerator.generateTagsForProject(any(), any(), any(), any())
        } throws RuntimeException("AI tags generation failure")

        val service = uut()

        // First run -> failure -> backoff recorded
        service.addAiTags()

        // Second run -> same project selected but is backed off -> should skip without calling generator again
        service.addAiTags()

        // Generator should be invoked only once (first run). Second run should skip early.
        verify(exactly = 1) { tagsGenerator.generateTagsForProject(any(), any(), any(), any()) }

        // No tags should be saved at all due to failure and then skip
        verify(exactly = 0) { projectTagRepository.saveAll(any<Iterable<TagEntity>>()) }
    }
}
