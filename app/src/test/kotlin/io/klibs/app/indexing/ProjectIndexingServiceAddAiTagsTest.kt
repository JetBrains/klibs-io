package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.integration.ai.ProjectTagsGenerator
import org.junit.jupiter.api.Test
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant

class ProjectIndexingServiceAddAiTagsTest {

    private val readmeService: ReadmeService = mockk()
    private val projectDescriptionGenerator: io.klibs.integration.ai.ProjectDescriptionGenerator = mockk()
    private val projectRepository: ProjectRepository = mockk()
    private val scmRepositoryRepository: ScmRepositoryRepository = mockk()
    private val projectTagsGenerator: ProjectTagsGenerator = mockk()
    private val projectTagRepository: ProjectTagRepository = mockk()
    private val descriptionBackoffProvider: BackoffProvider = BackoffProvider("descriptionBackoff", mockk())
    private val tagsBackoffProvider: BackoffProvider = BackoffProvider("descriptionBackoff", mockk())

    private fun uut() = ProjectIndexingService(
            readmeService = readmeService,
            projectDescriptionGenerator = projectDescriptionGenerator,
            projectRepository = projectRepository,
            scmRepositoryRepository = scmRepositoryRepository,
            projectTagsGenerator = projectTagsGenerator,
            projectTagRepository = projectTagRepository,
            descriptionBackoffProvider = descriptionBackoffProvider,
            tagsBackoffProvider = tagsBackoffProvider,
        )

    @Test
    fun `addAiTags should generate tags and save them with AI origin`() {
        val projectId = 101
        val scmRepoId = 202
        val project = ProjectEntity(
            id = projectId,
            scmRepoId = scmRepoId,
            description = "Project long description",
            latestVersion = "1.0.0",
            latestVersionTs = Instant.parse("2024-01-01T00:00:00Z")
        )
        every { projectRepository.findWithoutTags() } returns project

        val readme = "# Awesome lib\nSome README content"
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
            updatedAtTs = Instant.parse("2024-06-01T00:00:00Z"),
            minimizedReadme = readme
        )
        every { scmRepositoryRepository.findById(scmRepoId) } returns repo

        val generatedTags = listOf("kotlin", "testing", "http-client")
        every {
            projectTagsGenerator.generateTagsForProject(
                repo.name,
                project.description ?: "",
                repo.description ?: "",
                readme
            )
        } returns generatedTags

        val saveAllSlot: CapturingSlot<Iterable<TagEntity>> = slot()
        every { projectTagRepository.saveAll(capture(saveAllSlot)) } answers { saveAllSlot.captured.toList() }

        uut().addAiTags()

        val captor: CapturingSlot<Iterable<TagEntity>> = slot()
        verify { projectTagRepository.saveAll(capture(captor)) }
        val saved = captor.captured.toList()

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

        io.mockk.confirmVerified(scmRepositoryRepository)
        io.mockk.confirmVerified(readmeService)
        io.mockk.confirmVerified(projectTagsGenerator)
        io.mockk.confirmVerified(projectTagRepository)
    }

    @Test
    fun `addAiTags should backoff after failure and skip the same project on next run`() {
        val projectId = 11
        val scmRepoId = 22
        val project = ProjectEntity(
            id = projectId,
            scmRepoId = scmRepoId,
            description = "Desc",
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
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = Instant.parse("2024-06-01T00:00:00Z"),
            updatedAtTs = Instant.parse("2024-06-01T00:00:00Z"),
            minimizedReadme = "readme"
        )
        every { scmRepositoryRepository.findById(scmRepoId) } returns repo

        // Force a failure during tag generation
        every {
            projectTagsGenerator.generateTagsForProject(repo.name, project.description ?: "", repo.description ?: "", repo.minimizedReadme!!)
        } throws RuntimeException("AI tags generation failure")

        val service = uut()

        // First run -> failure -> backoff recorded
        service.addAiTags()

        // Second run -> same project selected but is backed off -> should skip without calling generator again
        service.addAiTags()

        // Generator should be invoked only once (first run). Second run should skip early.
        verify(exactly = 1) { projectTagsGenerator.generateTagsForProject(repo.name, project.description ?: "", repo.description ?: "", repo.minimizedReadme!!) }

        // No tags should be saved at all due to failure and then skip
        io.mockk.confirmVerified(projectTagRepository)
    }
}
