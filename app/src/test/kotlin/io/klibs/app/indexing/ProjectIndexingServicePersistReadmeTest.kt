package io.klibs.app.indexing

import io.klibs.app.service.TagsGenerationService
import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.service.OwnerMarkerService
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.core.readme.impl.ReadmeMinimizationProcessor
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Test
import io.klibs.core.readme.AndroidxReadmeProvider
import io.klibs.core.readme.GitHubIndexingReadmeContent
import io.klibs.core.readme.ReadmeContentBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectIndexingServicePersistReadmeTest {

    private val readmeService = mockk<ReadmeService>(relaxUnitFun = true)
    private val projectDescriptionGenerator = mockk<ProjectDescriptionGenerator>()
    private val projectRepository = mockk<ProjectRepository>()
    private val scmRepositoryRepository = mockk<ScmRepositoryRepository>()
    private val scmOwnerRepository = mockk<ScmOwnerRepository>()
    private val tagsGenerator = mockk<TagsGenerationService>()
    private val projectTagRepository = mockk<ProjectTagRepository>()
    private val gitHubIntegration = mockk<GitHubIntegration>()
    private val readmeContentBuilder = mockk<ReadmeContentBuilder>()
    private val androidxReadmeProvider = mockk<AndroidxReadmeProvider>()
    private val readmeMinimizer = mockk<ReadmeMinimizationProcessor>()
    private val descriptionBackoffProvider = BackoffProvider("descriptionBackoff", mockk(relaxed = true))
    private val tagsBackoffProvider = BackoffProvider("tagsBackoff", mockk(relaxed = true))
    private val ownerMarkerService = mockk<OwnerMarkerService>(relaxUnitFun = true)

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
        ownerMarkerService = ownerMarkerService,
    )

    @Test
    fun `save writes README when creating a project`() {
        val scmRepoId = 202
        val repoNativeId = 9090L
        val repoName = "repo-name"
        val defaultBranch = "main"
        val mavenArtifact = MavenArtifact(
            groupId = "io.test",
            artifactId = "repo-name",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = Instant.parse("2024-01-01T00:00:00Z")
        )
        val scmRepositoryEntity = ScmRepositoryEntity(
            id = scmRepoId,
            nativeId = repoNativeId,
            name = repoName,
            description = "Repo description",
            defaultBranch = defaultBranch,
            createdTs = Instant.parse("2020-01-01T00:00:00Z"),
            ownerId = 1,
            ownerType = io.klibs.core.owner.ScmOwnerType.AUTHOR,
            ownerLogin = "octocat",
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
            lastActivityTs = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAtTs = Instant.parse("2024-01-01T00:00:00Z")
        )
        val persistedProject = ProjectEntity(
            id = 303,
            scmRepoId = scmRepoId,
            ownerId = 1,
            name = repoName,
            description = null,
            minimizedReadme = null,
            latestVersion = mavenArtifact.version,
            latestVersionTs = requireNotNull(mavenArtifact.releasedAt)
        )

        every { projectRepository.findByNameAndScmRepoId(repoName, scmRepoId) } returns null
        every { projectRepository.insert(any()) } returns persistedProject
        every { scmRepositoryRepository.update(any()) } answers { firstArg<ScmRepositoryEntity>() }
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, Instant.EPOCH) } returns
            ReadmeFetchResult.Content("# Title")
        every {
            readmeContentBuilder.buildFromMarkdown(
                readmeMd = "# Title",
                nativeId = repoNativeId,
                ownerLogin = "octocat",
                repoName = repoName,
                defaultBranch = defaultBranch,
            )
        } returns GitHubIndexingReadmeContent(
            raw = "# Title",
            markdown = "# Title",
            html = "<h1>Title</h1>",
            minimized = "# Title",
        )

        val result = uut().save(mavenArtifact, scmRepositoryEntity)

        assertEquals(persistedProject.idNotNull, result.idNotNull)
        verify(exactly = 1) {
            readmeService.writeReadmeFiles(
                projectId = persistedProject.idNotNull,
                scmRepositoryId = scmRepositoryEntity.idNotNull,
                readmeMetadataEntity = null,
                rawContent = "# Title",
                mdContent = "# Title",
                htmlContent = "<h1>Title</h1>"
            )
        }

        val repoCaptor = mutableListOf<ScmRepositoryEntity>()
        verify(exactly = 1) { scmRepositoryRepository.update(capture(repoCaptor)) }
        assertEquals(true, repoCaptor.first().hasReadme)
        assertEquals(defaultBranch, repoCaptor.first().defaultBranch)
        assertEquals(repoNativeId, repoCaptor.first().nativeId)
        assertEquals(repoName, repoCaptor.first().name)
        assertEquals(scmRepoId, repoCaptor.first().idNotNull)
    }

    @Test
    fun `save persists androidx project with default description when no readme`() {
        val scmRepoId = 202
        val mavenArtifact = MavenArtifact(
            groupId = "androidx.paging",
            artifactId = "paging-common",
            version = "3.2.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = Instant.parse("2024-06-01T00:00:00Z")
        )
        val scmRepositoryEntity = androidxScmRepoEntity(scmRepoId)
        val persistedProject = ProjectEntity(
            id = 405,
            scmRepoId = scmRepoId,
            ownerId = 1,
            name = "paging",
            description = "This is androidx library, that was not documented or supported well. It is probably just a KMP module, that could be used on your own responsibility.",
            minimizedReadme = null,
            latestVersion = mavenArtifact.version,
            latestVersionTs = requireNotNull(mavenArtifact.releasedAt)
        )

        every { projectRepository.findByNameAndScmRepoId("paging", scmRepoId) } returns null
        every { androidxReadmeProvider.resolve("paging", "md.raw") } returns null
        every { projectRepository.insert(any()) } returns persistedProject

        val result = uut().save(mavenArtifact, scmRepositoryEntity)

        assertEquals(405, result.idNotNull)

        val projectCaptor = mutableListOf<ProjectEntity>()
        verify(exactly = 1) { projectRepository.insert(capture(projectCaptor)) }
        val inserted = projectCaptor.first()
        assertEquals("paging", inserted.name)
        assertNull(inserted.minimizedReadme)
        assertEquals(
            "This is androidx library, that was not documented or supported well. It is probably just a KMP module, that could be used on your own responsibility.",
            inserted.description
        )

        verify(exactly = 0) { readmeMinimizer.process(any(), any(), any(), any()) }
        verify(exactly = 0) { readmeService.writeReadmeFiles(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { scmRepositoryRepository.update(any()) }
        verify(exactly = 0) { gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()) }
    }

    @Test
    fun `save updates existing androidx project when new artifact arrives`() {
        val scmRepoId = 202
        val mavenArtifact = MavenArtifact(
            groupId = "androidx.compose.animation",
            artifactId = "animation-core",
            version = "1.7.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = Instant.parse("2025-03-01T00:00:00Z")
        )
        val scmRepositoryEntity = androidxScmRepoEntity(scmRepoId)
        val existingProject = ProjectEntity(
            id = 404,
            scmRepoId = scmRepoId,
            ownerId = 1,
            name = "compose",
            description = null,
            minimizedReadme = "minimized readme",
            latestVersion = "1.5.0",
            latestVersionTs = Instant.parse("2024-06-01T00:00:00Z")
        )
        val updatedProject = existingProject.copy(
            latestVersion = mavenArtifact.version,
            latestVersionTs = requireNotNull(mavenArtifact.releasedAt)
        )

        every { projectRepository.findByNameAndScmRepoId("compose", scmRepoId) } returns existingProject
        every {
            projectRepository.updateLatestVersion(
                id = existingProject.idNotNull,
                latestVersion = mavenArtifact.version,
                latestVersionTs = requireNotNull(mavenArtifact.releasedAt)
            )
        } returns updatedProject

        val result = uut().save(mavenArtifact, scmRepositoryEntity)

        assertEquals(existingProject.idNotNull, result.idNotNull)
        assertEquals("1.7.0", result.latestVersion)
        assertEquals(Instant.parse("2025-03-01T00:00:00Z"), result.latestVersionTs)

        verify(exactly = 1) { projectRepository.findByNameAndScmRepoId("compose", scmRepoId) }
        verify(exactly = 1) {
            projectRepository.updateLatestVersion(
                id = existingProject.idNotNull,
                latestVersion = "1.7.0",
                latestVersionTs = Instant.parse("2025-03-01T00:00:00Z")
            )
        }
        verify(exactly = 0) { projectRepository.insert(any()) }
        verify(exactly = 0) { androidxReadmeProvider.resolve(any(), any()) }
        verify(exactly = 0) { projectRepository.updateMinimizedReadme(any(), any()) }
    }

    private fun androidxScmRepoEntity(scmRepoId: Int) = ScmRepositoryEntity(
        id = scmRepoId,
        nativeId = 8080L,
        name = "androidx-repo",
        description = "AndroidX repo",
        defaultBranch = "main",
        createdTs = Instant.parse("2020-01-01T00:00:00Z"),
        ownerId = 1,
        ownerType = io.klibs.core.owner.ScmOwnerType.AUTHOR,
        ownerLogin = "androidx",
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
        lastActivityTs = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAtTs = Instant.parse("2024-01-01T00:00:00Z")
    )
}