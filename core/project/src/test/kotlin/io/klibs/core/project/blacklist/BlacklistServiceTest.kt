package io.klibs.core.project.blacklist

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.project.repository.ProjectRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.every
import io.mockk.verify
import io.mockk.confirmVerified
import io.mockk.mockk
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlacklistServiceTest {

    private lateinit var blacklistRepository: BlacklistRepository
    private lateinit var packageRepository: PackageRepository
    private lateinit var projectRepository: ProjectRepository

    private lateinit var uut: BlacklistService

    private val testGroupId = "test.group"
    private val testArtifactId = "test-artifact"
    private val testProjectId = 123
    private val testVersion = "1.0.0"
    private val testReleaseTs = Instant.now()

    @BeforeEach
    fun setUp() {
        // Create relaxed mocks to avoid explicit stubbing of Unit functions
        blacklistRepository = mockk(relaxUnitFun = true)
        packageRepository = mockk(relaxed = true)
        projectRepository = mockk(relaxUnitFun = true)

        uut = BlacklistService(blacklistRepository, packageRepository, projectRepository)

        every { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) } returns true
        // Default: not banned
        every { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) } returns false
        // Default: no connected projects
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns emptySet()
        every { projectRepository.findProjectsByPackages(testGroupId, null) } returns emptySet()
    }

    private fun mockPackageDoesNotExist() {
        every { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) } returns false
    }

    private fun mockPackageIsAlreadyBanned() {
        every { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) } returns true
    }

    private fun verifyNoFurtherInteractions() {
        verify(exactly = 0) { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify(exactly = 0) { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
    }

    @Test
    fun testSuccessfulBan() {
        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify { blacklistRepository.removeBannedPackages() }
    }

    @Test
    fun testBanNonExistentPackage() {
        mockPackageDoesNotExist()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertFalse(result)
        verify { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verifyNoFurtherInteractions()
    }

    @Test
    fun testBanAlreadyBannedPackage() {
        mockPackageIsAlreadyBanned()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertFalse(result)
        verify { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verifyNoFurtherInteractions()
    }

    @Test
    fun testSuccessfulBanByGroupWithReason() {
        val reason = "Security vulnerability"
        val result = uut.banByGroup(testGroupId, reason)

        assertTrue(result)
        verify { blacklistRepository.addToBannedPackages(testGroupId, null, reason) }
        verify { blacklistRepository.removeBannedPackages(testGroupId, null) }
        verify { blacklistRepository.removeBannedPackages() }
    }

    @Test
    fun testProjectsUpdatedWhenPackageBanned() {
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns setOf(testProjectId)

        val latestPackage = mockk<PackageEntity>()
        every { latestPackage.version } returns testVersion
        every { latestPackage.releaseTs } returns testReleaseTs
        every { packageRepository.findLatestByProjectId(testProjectId) } returns listOf(latestPackage)
        every { projectRepository.updateLatestVersion(testProjectId, testVersion, testReleaseTs) } returns (
            io.klibs.core.project.ProjectEntity(
                id = testProjectId,
                scmRepoId = 0,
                description = null,
                latestVersion = testVersion,
                latestVersionTs = testReleaseTs
            )
        )

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify { blacklistRepository.removeBannedPackages() }

        verify { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) }
        verify { projectRepository.updateLatestVersion(testProjectId, testVersion, testReleaseTs) }
    }

    @Test
    fun testProjectsUpdatedWhenGroupBanned() {
        every { projectRepository.findProjectsByPackages(testGroupId, null) } returns setOf(testProjectId)

        // Mock package repository to return a latest package for the project
        val latestPackage = mockk<PackageEntity>()
        every { latestPackage.version } returns testVersion
        every { latestPackage.releaseTs } returns testReleaseTs
        every { packageRepository.findLatestByProjectId(testProjectId) } returns listOf(latestPackage)
        every { projectRepository.updateLatestVersion(testProjectId, testVersion, testReleaseTs) } returns (
            io.klibs.core.project.ProjectEntity(
                id = testProjectId,
                scmRepoId = 0,
                description = null,
                latestVersion = testVersion,
                latestVersionTs = testReleaseTs
            )
        )

        val result = uut.banByGroup(testGroupId, null)

        assertTrue(result)
        verify { blacklistRepository.addToBannedPackages(testGroupId, null, null) }
        verify { blacklistRepository.removeBannedPackages(testGroupId, null) }
        verify { blacklistRepository.removeBannedPackages() }

        verify { projectRepository.findProjectsByPackages(testGroupId, null) }
        verify { projectRepository.updateLatestVersion(testProjectId, testVersion, testReleaseTs) }
    }

    @Test
    fun testNoProjectsUpdatedWhenNoLatestPackages() {
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns setOf(testProjectId)
        every { packageRepository.findLatestByProjectId(testProjectId) } returns emptyList()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify { blacklistRepository.removeBannedPackages() }

        // Verify that the project repository was called to find projects by packages
        verify { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) }
        confirmVerified(projectRepository)
    }

    @Test
    fun testNoProjectsUpdatedWhenNoConnectedProjects() {
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns emptySet()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify { blacklistRepository.removeBannedPackages() }

        // Verify that the project repository was called to only find projects by packages
        // and for nothing else
        verify { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) }
        confirmVerified(projectRepository)
    }
}
