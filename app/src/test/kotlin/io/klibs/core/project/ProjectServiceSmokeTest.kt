package io.klibs.core.project

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.project.entity.Marker
import io.klibs.core.project.enums.MarkerType
import io.klibs.core.project.repository.MarkerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.core.project.repository.TagRepository
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(classes = [ProjectService::class])
@ActiveProfiles("test")
class ProjectServiceSmokeTest {

    @Autowired
    private lateinit var uut: ProjectService

    @MockkBean
    private lateinit var packageService: PackageService

    @MockkBean
    private lateinit var readmeService: ReadmeService

    @MockkBean
    private lateinit var projectRepository: ProjectRepository

    @MockkBean
    private lateinit var packageRepository: PackageRepository

    @MockkBean
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockkBean
    private lateinit var markerRepository: MarkerRepository

    @MockkBean
    private lateinit var tagRepository: TagRepository

    @Test
    fun `getProjectDetailsByName returns null when project has no packages`() {
        // Arrange
        val ownerLogin = "testOwner"
        val projectName = "testProject"
        val scmRepoId = 1
        val now = Instant.now()

        val scmRepositoryEntity = ScmRepositoryEntity(
            id = scmRepoId,
            nativeId = 12345L,
            name = projectName,
            description = "Test project",
            defaultBranch = "main",
            createdTs = now,
            ownerId = 1,
            ownerType = ScmOwnerType.AUTHOR,
            ownerLogin = ownerLogin,
            homepage = null,
            hasGhPages = false,
            hasIssues = false,
            hasWiki = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = now,
            updatedAtTs = now,
            minimizedReadme = null
        )

        val projectEntity = ProjectEntity(
            id = 1,
            scmRepoId = scmRepoId,
            description = "Test project description",
            latestVersion = "1.0.0",
            latestVersionTs = now,
        )

        // Mock repository responses
        every { scmRepositoryRepository.findByName(ownerLogin, projectName) } returns scmRepositoryEntity
        every { projectRepository.findByScmRepoId(scmRepoId) } returns projectEntity
        every { packageRepository.existsByProjectId(projectEntity.idNotNull) } returns false

        // Default stub for tags
        every { tagRepository.getTagsByProjectId(any()) } returns emptyList()

        // Act
        val result = uut.getProjectDetailsByName(ownerLogin, projectName)

        // Assert
        assertNull(result, "Project details should be null when project has no packages")
    }

    @Test
    fun `getProjectDetailsById includes project markers`() {
        val projectId = 1
        val scmRepoId = 2
        val now = Instant.now()

        val scmRepositoryEntity = ScmRepositoryEntity(
            id = scmRepoId,
            nativeId = 12345L,
            name = "testProject",
            description = "Test project",
            defaultBranch = "main",
            createdTs = now,
            ownerId = 1,
            ownerType = ScmOwnerType.AUTHOR,
            ownerLogin = "testOwner",
            homepage = null,
            hasGhPages = false,
            hasIssues = false,
            hasWiki = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = now,
            updatedAtTs = now,
            minimizedReadme = null
        )

        val projectEntity = ProjectEntity(
            id = projectId,
            scmRepoId = scmRepoId,
            description = "Test project description",
            latestVersion = "1.0.0",
            latestVersionTs = now,
        )

        val projectMarkers = listOf(
            Marker(projectId = projectId, type = MarkerType.FEATURED),
            Marker(projectId = projectId, type = MarkerType.GRANT_WINNER_2023)
        )

        val platforms = listOf(PackagePlatform.JVM, PackagePlatform.JS)

        every { projectRepository.findById(projectId) } returns projectEntity
        every { scmRepositoryRepository.findById(scmRepoId) } returns scmRepositoryEntity
        every { packageRepository.findPlatformsOf(projectId) } returns platforms
        every { markerRepository.findAllByProjectId(projectId) } returns projectMarkers

        // Default stub for tags
        every { tagRepository.getTagsByProjectId(any()) } returns emptyList()

        val foundProject = uut.getProjectDetailsById(projectId)

        assertNotNull(foundProject)
        assertEquals(projectId, foundProject.id, "Project ID should match")
        assertEquals(2, foundProject.markers.size, "Project should have 2 markers")
        assertEquals(listOf(MarkerType.FEATURED, MarkerType.GRANT_WINNER_2023), foundProject.markers)
    }

    @Test
    fun `getProjectDetailsByName includes project markers`() {
        // Arrange
        val ownerLogin = "testOwner"
        val projectName = "testProject"
        val projectId = 1
        val scmRepoId = 2
        val now = Instant.now()

        val scmRepositoryEntity = ScmRepositoryEntity(
            id = scmRepoId,
            nativeId = 12345L,
            name = projectName,
            description = "Test project",
            defaultBranch = "main",
            createdTs = now,
            ownerId = 1,
            ownerType = ScmOwnerType.AUTHOR,
            ownerLogin = ownerLogin,
            homepage = null,
            hasGhPages = false,
            hasIssues = false,
            hasWiki = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = now,
            updatedAtTs = now,
            minimizedReadme = null
        )

        val projectEntity = ProjectEntity(
            id = projectId,
            scmRepoId = scmRepoId,
            description = "Test project description",
            latestVersion = "1.0.0",
            latestVersionTs = now,
        )

        val projectMarkers = listOf(
            Marker(projectId = projectId, type = MarkerType.FEATURED),
            Marker(projectId = projectId, type = MarkerType.GRANT_WINNER_2023)
        )

        val platforms = listOf(PackagePlatform.JVM, PackagePlatform.JS)

        // Mock repository responses
        every { scmRepositoryRepository.findByName(ownerLogin, projectName) } returns scmRepositoryEntity
        every { projectRepository.findByScmRepoId(scmRepoId) } returns projectEntity
        every { packageRepository.existsByProjectId(projectEntity.idNotNull) } returns true
        every { packageRepository.findPlatformsOf(projectEntity.idNotNull) } returns platforms
        every { markerRepository.findAllByProjectId(projectEntity.idNotNull) } returns projectMarkers

        val result = uut.getProjectDetailsByName(ownerLogin, projectName)

        assertNotNull(result)
        assertEquals(projectId, result.id, "Project ID should match")
        assertEquals(2, result.markers.size, "Project should have 2 markers")
        assertEquals(listOf(MarkerType.FEATURED, MarkerType.GRANT_WINNER_2023), result.markers)
    }
}
