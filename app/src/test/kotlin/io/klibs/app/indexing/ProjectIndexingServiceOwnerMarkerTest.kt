package io.klibs.app.indexing

import io.klibs.app.service.TagsGenerationService
import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.service.OwnerMarkerService
import io.klibs.core.readme.ReadmeContentBuilder
import io.klibs.core.readme.service.ReadmeService
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

class ProjectIndexingServiceOwnerMarkerTest {

    private val readmeService = mockk<ReadmeService>(relaxUnitFun = true)
    private val projectDescriptionGenerator = mockk<ProjectDescriptionGenerator>()
    private val projectRepository = mockk<ProjectRepository>()
    private val scmRepositoryRepository = mockk<ScmRepositoryRepository>()
    private val scmOwnerRepository = mockk<ScmOwnerRepository>()
    private val tagsGenerator = mockk<TagsGenerationService>()
    private val projectTagRepository = mockk<ProjectTagRepository>()
    private val gitHubIntegration = mockk<GitHubIntegration>()
    private val readmeContentBuilder = mockk<ReadmeContentBuilder>()
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
    fun `new project assigns owner markers`() {
        val scmRepo = scmRepoEntity(ownerLogin = "JetBrains", name = "kotlinx.coroutines")
        val artifact = mavenArtifact(groupId = "org.jetbrains.kotlinx", artifactId = "kotlinx-coroutines-core")

        every { projectRepository.findByNameAndScmRepoId("kotlinx.coroutines", scmRepo.idNotNull) } returns null
        every { projectRepository.insert(any()) } answers { firstArg<ProjectEntity>().copy(id = 501) }
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(scmRepo.nativeId, Instant.EPOCH) } returns
            ReadmeFetchResult.NotFound

        uut().save(artifact, scmRepo)

        verify(exactly = 1) { ownerMarkerService.assignOwnerMarkers(501, "JetBrains") }
    }

    @Test
    fun `new androidx project assigns owner markers`() {
        val scmRepo = scmRepoEntity(ownerLogin = "androidx", name = "androidx")
        val artifact = mavenArtifact(groupId = "androidx.paging", artifactId = "paging-common")

        every { projectRepository.findByNameAndScmRepoId("paging", scmRepo.idNotNull) } returns null
        every { projectRepository.insert(any()) } answers { firstArg<ProjectEntity>().copy(id = 502) }

        uut().save(artifact, scmRepo)

        verify(exactly = 1) { ownerMarkerService.assignOwnerMarkers(502, "androidx") }
        verify(exactly = 0) { gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()) }
    }

    @Test
    fun `existing project does not assign owner markers`() {
        val scmRepo = scmRepoEntity(ownerLogin = "Kotlin", name = "kotlinx-io")
        val artifact = mavenArtifact(groupId = "org.jetbrains.kotlinx", artifactId = "kotlinx-io-core")
        val existing = ProjectEntity(
            id = 503,
            scmRepoId = scmRepo.idNotNull,
            ownerId = scmRepo.ownerId,
            name = "kotlinx-io",
            description = null,
            minimizedReadme = null,
            latestVersion = artifact.version,
            latestVersionTs = requireNotNull(artifact.releasedAt),
        )

        every { projectRepository.findByNameAndScmRepoId("kotlinx-io", scmRepo.idNotNull) } returns existing

        uut().save(artifact, scmRepo)

        verify(exactly = 0) { ownerMarkerService.assignOwnerMarkers(any(), any()) }
        verify(exactly = 0) { projectRepository.insert(any()) }
    }

    private fun mavenArtifact(groupId: String, artifactId: String) = MavenArtifact(
        groupId = groupId,
        artifactId = artifactId,
        version = "1.0.0",
        scraperType = ScraperType.CENTRAL_SONATYPE,
        releasedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )

    private fun scmRepoEntity(ownerLogin: String, name: String) = ScmRepositoryEntity(
        id = 202,
        nativeId = 9090L,
        name = name,
        description = null,
        defaultBranch = "main",
        createdTs = Instant.parse("2020-01-01T00:00:00Z"),
        ownerId = 1,
        ownerType = ScmOwnerType.ORGANIZATION,
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
        lastActivityTs = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAtTs = Instant.parse("2024-01-01T00:00:00Z"),
    )
}
