package io.klibs.app.service.impl

import io.klibs.app.service.BlacklistService
import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.BlacklistRepository
import io.klibs.core.project.repository.ProjectRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class BlacklistServiceTest {

    @MockK(relaxed = true)
    private lateinit var blacklistRepository: BlacklistRepository

    @MockK
    private lateinit var packageRepository: PackageRepository

    @MockK(relaxed = true)
    private lateinit var projectRepository: ProjectRepository

    private lateinit var uut: BlacklistService

    private val testGroupId = "test.group"
    private val testArtifactId = "test-artifact"
    private val testProjectId = 123
    private val testVersion = "1.0.0"
    private val testReleaseTs = Instant.now()

    @BeforeEach
    fun setUp() {
        uut = DefaultBlacklistService(
            blacklistRepository,
            packageRepository,
            projectRepository
        )

        every { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) } returns true

        // Mock empty lists by default to avoid NPEs
        every { packageRepository.findByGroupIdAndArtifactIdOrderByReleaseTsDesc(any(), any()) } returns emptyList()
        every { packageRepository.findLatestByProjectId(any()) } returns emptyList()
    }

    private fun mockPackageDoesNotExist() {
        every { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) } returns false
    }

    private fun mockPackageIsAlreadyBanned() {
        every { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) } returns true
    }

    private fun verifyNoFurtherInteractions() {
        verify(exactly = 0) { blacklistRepository.addToBannedPackages(any(), any(), any()) }
        verify(exactly = 0) { blacklistRepository.removeBannedPackages(any(), any()) }
    }

    @Test
    fun testSuccessfulBan() {
        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(exactly = 1) { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages() }
    }

    @Test
    fun testBanNonExistentPackage() {
        mockPackageDoesNotExist()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertFalse(result)
        verify(exactly = 1) { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verifyNoFurtherInteractions()
    }

    @Test
    fun testBanAlreadyBannedPackage() {
        mockPackageIsAlreadyBanned()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertFalse(result)
        verify(exactly = 1) { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verifyNoFurtherInteractions()
    }

    @Test
    fun testSuccessfulBanByGroupWithReason() {
        val reason = "Security vulnerability"
        val result = uut.banByGroup(testGroupId, reason)

        assertTrue(result)
        verify(exactly = 1) { blacklistRepository.addToBannedPackages(testGroupId, null, reason) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages(testGroupId, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages() }
    }

    @Test
    fun testProjectsUpdatedWhenPackageBanned() {
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns setOf(testProjectId)

        val latestPackage = mockk<PackageEntity>()
        every { latestPackage.version } returns testVersion
        every { latestPackage.releaseTs } returns testReleaseTs
        every { packageRepository.findLatestByProjectId(testProjectId) } returns listOf(latestPackage)

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(exactly = 1) { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages() }

        verify(exactly = 1) { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) }
        verify(exactly = 1) { projectRepository.updateLatestVersion(testProjectId, testVersion, testReleaseTs) }
    }

    @Test
    fun testProjectsUpdatedWhenGroupBanned() {
        every { projectRepository.findProjectsByPackages(testGroupId, null) } returns setOf(testProjectId)

        // Mock package repository to return a latest package for the project
        val latestPackage = mockk<PackageEntity>()
        every { latestPackage.version } returns testVersion
        every { latestPackage.releaseTs } returns testReleaseTs
        every { packageRepository.findLatestByProjectId(testProjectId) } returns listOf(latestPackage)

        val result = uut.banByGroup(testGroupId, null)

        assertTrue(result)
        verify(exactly = 1) { blacklistRepository.addToBannedPackages(testGroupId, null, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages(testGroupId, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages() }

        verify(exactly = 1) { projectRepository.findProjectsByPackages(testGroupId, null) }
        verify(exactly = 1) { projectRepository.updateLatestVersion(testProjectId, testVersion, testReleaseTs) }
    }

    @Test
    fun testNoProjectsUpdatedWhenNoLatestPackages() {
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns setOf(testProjectId)
        every { packageRepository.findLatestByProjectId(testProjectId) } returns emptyList()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(exactly = 1) { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages() }

        // Verify that the project repository was called to find projects by packages
        verify(exactly = 1) { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) }
        confirmVerified(projectRepository)
    }

    @Test
    fun testNoProjectsUpdatedWhenNoConnectedProjects() {
        every { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) } returns emptySet()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(exactly = 1) { blacklistRepository.checkPackageExists(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.checkPackageBanned(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.addToBannedPackages(testGroupId, testArtifactId, null) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages(testGroupId, testArtifactId) }
        verify(exactly = 1) { blacklistRepository.removeBannedPackages() }

        // Verify that the project repository was called to only find projects by packages
        // and for nothing else
        verify(exactly = 1) { projectRepository.findProjectsByPackages(testGroupId, testArtifactId) }
        confirmVerified(projectRepository)
    }
}
