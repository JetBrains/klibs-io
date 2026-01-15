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
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyInt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @MockBean
    private lateinit var packageService: PackageService

    @MockBean
    private lateinit var readmeService: ReadmeService

    @MockBean
    private lateinit var projectRepository: ProjectRepository

    @MockBean
    private lateinit var packageRepository: PackageRepository

    @MockBean
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockBean
    private lateinit var markerRepository: MarkerRepository

    @MockBean
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
        `when`(scmRepositoryRepository.findByName(ownerLogin, projectName)).thenReturn(scmRepositoryEntity)
        `when`(projectRepository.findByScmRepoId(scmRepoId)).thenReturn(projectEntity)
        `when`(packageRepository.existsByProjectId(projectEntity.idNotNull)).thenReturn(false)

        // Default stub for tags
        `when`(tagRepository.getTagsByProjectId(anyInt())).thenReturn(emptyList())

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

        `when`(projectRepository.findById(projectId)).thenReturn(projectEntity)
        `when`(scmRepositoryRepository.findById(scmRepoId)).thenReturn(scmRepositoryEntity)
        `when`(packageRepository.findPlatformsOf(projectId)).thenReturn(platforms)
        `when`(markerRepository.findAllByProjectId(projectId)).thenReturn(projectMarkers)

        // Default stub for tags
        `when`(tagRepository.getTagsByProjectId(anyInt())).thenReturn(emptyList())

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
        `when`(scmRepositoryRepository.findByName(ownerLogin, projectName)).thenReturn(scmRepositoryEntity)
        `when`(projectRepository.findByScmRepoId(scmRepoId)).thenReturn(projectEntity)
        `when`(packageRepository.existsByProjectId(projectEntity.idNotNull)).thenReturn(true)
        `when`(packageRepository.findPlatformsOf(projectEntity.idNotNull)).thenReturn(platforms)
        `when`(markerRepository.findAllByProjectId(projectEntity.idNotNull)).thenReturn(projectMarkers)

        val result = uut.getProjectDetailsByName(ownerLogin, projectName)

        assertNotNull(result)
        assertEquals(projectId, result.id, "Project ID should match")
        assertEquals(2, result.markers.size, "Project should have 2 markers")
        assertEquals(listOf(MarkerType.FEATURED, MarkerType.GRANT_WINNER_2023), result.markers)
    }
}
