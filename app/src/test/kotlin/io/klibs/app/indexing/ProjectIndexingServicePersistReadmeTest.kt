package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.service.ReadmeServiceDispatcher
import io.klibs.core.readme.impl.ReadmeMinimizationProcessor
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.ai.ProjectTagsGenerator
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Test
import io.klibs.core.readme.AndroidxReadmeProvider
import io.klibs.core.readme.GitHubIndexingReadmeContent
import io.klibs.core.readme.ReadmeContentBuilder
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectIndexingServicePersistReadmeTest {

    private val readmeServiceDispatcher: ReadmeServiceDispatcher = mock()
    private val projectDescriptionGenerator: ProjectDescriptionGenerator = mock()
    private val projectRepository: ProjectRepository = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val scmOwnerRepository: io.klibs.core.owner.ScmOwnerRepository = mock()
    private val projectTagsGenerator: ProjectTagsGenerator = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val gitHubIntegration: GitHubIntegration = mock()
    private val readmeContentBuilder: ReadmeContentBuilder = mock()
    private val androidxReadmeProvider: AndroidxReadmeProvider = mock()
    private val readmeMinimizer: ReadmeMinimizationProcessor = mock()
    private val descriptionBackoffProvider: BackoffProvider = BackoffProvider("descriptionBackoff", mock())
    private val tagsBackoffProvider: BackoffProvider = BackoffProvider("tagsBackoff", mock())

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
        descriptionBackoffProvider = descriptionBackoffProvider,
        tagsBackoffProvider = tagsBackoffProvider,
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

        whenever(projectRepository.findByNameAndScmRepoId(repoName, scmRepoId)).thenReturn(null)
        whenever(projectRepository.insert(any())).thenReturn(persistedProject)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(repoNativeId, Instant.EPOCH))
            .thenReturn(ReadmeFetchResult.Content("# Title"))
        whenever(
            readmeContentBuilder.buildFromMarkdown(
                readmeMd = "# Title",
                nativeId = repoNativeId,
                ownerLogin = "octocat",
                repoName = repoName,
                defaultBranch = defaultBranch,
            )
        ).thenReturn(
            GitHubIndexingReadmeContent(
                markdown = "# Title",
                html = "<h1>Title</h1>",
                minimized = "# Title",
            )
        )

        val result = uut().save(mavenArtifact, scmRepositoryEntity)

        assertEquals(persistedProject.idNotNull, result.idNotNull)
        verify(readmeServiceDispatcher).writeReadmeFiles(
            projectId = persistedProject.idNotNull,
            mdContent = "# Title",
            htmlContent = "<h1>Title</h1>"
        )

        val repoCaptor = argumentCaptor<ScmRepositoryEntity>()
        verify(scmRepositoryRepository).update(repoCaptor.capture())
        assertEquals(true, repoCaptor.firstValue.hasReadme)
        assertEquals(defaultBranch, repoCaptor.firstValue.defaultBranch)
        assertEquals(repoNativeId, repoCaptor.firstValue.nativeId)
        assertEquals(repoName, repoCaptor.firstValue.name)
        assertEquals(scmRepoId, repoCaptor.firstValue.idNotNull)
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

        whenever(projectRepository.findByNameAndScmRepoId("paging", scmRepoId)).thenReturn(null)
        whenever(androidxReadmeProvider.resolve("paging", "md.raw")).thenReturn(null)
        whenever(projectRepository.insert(any())).thenReturn(persistedProject)

        val result = uut().save(mavenArtifact, scmRepositoryEntity)

        assertEquals(405, result.idNotNull)

        val projectCaptor = argumentCaptor<ProjectEntity>()
        verify(projectRepository).insert(projectCaptor.capture())
        val inserted = projectCaptor.firstValue
        assertEquals("paging", inserted.name)
        assertNull(inserted.minimizedReadme)
        assertEquals(
            "This is androidx library, that was not documented or supported well. It is probably just a KMP module, that could be used on your own responsibility.",
            inserted.description
        )

        verify(readmeMinimizer, never()).process(any(), any(), any(), any())
        verify(readmeServiceDispatcher, never()).writeReadmeFiles(any(), any(), any())
        verify(scmRepositoryRepository, never()).update(any())
        verify(gitHubIntegration, never()).getReadmeWithModifiedSinceCheck(any(), any())
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

        whenever(projectRepository.findByNameAndScmRepoId("compose", scmRepoId)).thenReturn(existingProject)
        whenever(
            projectRepository.updateLatestVersion(
                id = existingProject.idNotNull,
                latestVersion = mavenArtifact.version,
                latestVersionTs = requireNotNull(mavenArtifact.releasedAt)
            )
        ).thenReturn(updatedProject)

        val result = uut().save(mavenArtifact, scmRepositoryEntity)

        assertEquals(existingProject.idNotNull, result.idNotNull)
        assertEquals("1.7.0", result.latestVersion)
        assertEquals(Instant.parse("2025-03-01T00:00:00Z"), result.latestVersionTs)

        verify(projectRepository).findByNameAndScmRepoId("compose", scmRepoId)
        verify(projectRepository).updateLatestVersion(
            id = existingProject.idNotNull,
            latestVersion = "1.7.0",
            latestVersionTs = Instant.parse("2025-03-01T00:00:00Z")
        )
        verify(projectRepository, never()).insert(any())
        verify(androidxReadmeProvider, never()).resolve(any(), any())
        verify(projectRepository, never()).updateMinimizedReadme(any(), any())
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
        hasReadme = false,
        licenseKey = null,
        licenseName = null,
        stars = 0,
        openIssues = 0,
        lastActivityTs = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAtTs = Instant.parse("2024-01-01T00:00:00Z")
    )
}