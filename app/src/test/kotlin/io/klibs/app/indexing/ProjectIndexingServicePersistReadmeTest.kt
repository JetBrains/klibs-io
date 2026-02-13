package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.ai.ProjectTagsGenerator
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class ProjectIndexingServicePersistReadmeTest {

    private val readmeService: ReadmeService = mock()
    private val projectDescriptionGenerator: ProjectDescriptionGenerator = mock()
    private val projectRepository: ProjectRepository = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val projectTagsGenerator: ProjectTagsGenerator = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val gitHubIntegration: GitHubIntegration = mock()
    private val readmeContentBuilder: ReadmeContentBuilder = mock()
    private val descriptionBackoffProvider: BackoffProvider = BackoffProvider("descriptionBackoff", mock())
    private val tagsBackoffProvider: BackoffProvider = BackoffProvider("tagsBackoff", mock())

    private fun uut() = ProjectIndexingService(
        readmeService = readmeService,
        projectDescriptionGenerator = projectDescriptionGenerator,
        projectRepository = projectRepository,
        scmRepositoryRepository = scmRepositoryRepository,
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
        verify(readmeService).writeReadmeFiles(
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
}