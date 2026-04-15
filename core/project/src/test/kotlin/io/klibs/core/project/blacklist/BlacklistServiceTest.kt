package io.klibs.core.project.blacklist

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.project.repository.ProjectRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlacklistServiceTest {

    @Mock
    private lateinit var blacklistRepository: BlacklistRepository

    @Mock
    private lateinit var packageRepository: PackageRepository

    @Mock
    private lateinit var projectRepository: ProjectRepository

    private lateinit var uut: BlacklistService

    private val testGroupId = "test.group"
    private val testArtifactId = "test-artifact"
    private val testProjectId = 123
    private val testVersion = "1.0.0"
    private val testReleaseTs = Instant.now()

    @BeforeEach
    fun setUp() {
        uut = BlacklistService(
            blacklistRepository,
            packageRepository,
            projectRepository
        )

        `when`(blacklistRepository.checkPackageExists(testGroupId, testArtifactId)).thenReturn(true)

        // Mock empty lists by default to avoid NPEs
        `when`(packageRepository.findByGroupIdAndArtifactIdOrderByReleaseTsDesc(anyString(), anyString())).thenReturn(emptyList())
        `when`(packageRepository.findLatestByProjectId(anyInt())).thenReturn(emptyList())
    }

    private fun mockPackageDoesNotExist() {
        `when`(blacklistRepository.checkPackageExists(testGroupId, testArtifactId)).thenReturn(false)
    }

    private fun mockPackageIsAlreadyBanned() {
        `when`(blacklistRepository.checkPackageBanned(testGroupId, testArtifactId)).thenReturn(true)
    }

    private fun verifyNoFurtherInteractions() {
        verify(blacklistRepository, never()).addToBannedPackages(anyString(), anyString(), anyString())
        verify(blacklistRepository, never()).removeBannedPackages(anyString(), anyString())
    }

    @Test
    fun testSuccessfulBan() {
        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(blacklistRepository).checkPackageExists(testGroupId, testArtifactId)
        verify(blacklistRepository).checkPackageBanned(testGroupId, testArtifactId)
        verify(blacklistRepository).addToBannedPackages(testGroupId, testArtifactId, null)
        verify(blacklistRepository).removeBannedPackages(testGroupId, testArtifactId)
        verify(blacklistRepository).removeBannedPackages()
    }

    @Test
    fun testBanNonExistentPackage() {
        mockPackageDoesNotExist()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertFalse(result)
        verify(blacklistRepository).checkPackageExists(testGroupId, testArtifactId)
        verifyNoFurtherInteractions()
    }

    @Test
    fun testBanAlreadyBannedPackage() {
        mockPackageIsAlreadyBanned()

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertFalse(result)
        verify(blacklistRepository).checkPackageExists(testGroupId, testArtifactId)
        verifyNoFurtherInteractions()
    }

    @Test
    fun testSuccessfulBanByGroupWithReason() {
        val reason = "Security vulnerability"
        val result = uut.banByGroup(testGroupId, reason)

        assertTrue(result)
        verify(blacklistRepository).addToBannedPackages(testGroupId, null, reason)
        verify(blacklistRepository).removeBannedPackages(testGroupId, null)
        verify(blacklistRepository).removeBannedPackages()
    }

    @Test
    fun testProjectsUpdatedWhenPackageBanned() {
        `when`(projectRepository.findProjectsByPackages(testGroupId, testArtifactId)).thenReturn(setOf(testProjectId))

        val latestPackage = mock(PackageEntity::class.java)
        `when`(latestPackage.version).thenReturn(testVersion)
        `when`(latestPackage.releaseTs).thenReturn(testReleaseTs)
        `when`(packageRepository.findLatestByProjectId(testProjectId)).thenReturn(listOf(latestPackage))

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(blacklistRepository).checkPackageExists(testGroupId, testArtifactId)
        verify(blacklistRepository).checkPackageBanned(testGroupId, testArtifactId)
        verify(blacklistRepository).addToBannedPackages(testGroupId, testArtifactId, null)
        verify(blacklistRepository).removeBannedPackages(testGroupId, testArtifactId)
        verify(blacklistRepository).removeBannedPackages()

        verify(projectRepository).findProjectsByPackages(testGroupId, testArtifactId)
        verify(projectRepository).updateLatestVersion(testProjectId, testVersion, testReleaseTs)
    }

    @Test
    fun testProjectsUpdatedWhenGroupBanned() {
        `when`(projectRepository.findProjectsByPackages(testGroupId, null)).thenReturn(setOf(testProjectId))

        // Mock package repository to return a latest package for the project
        val latestPackage = mock(PackageEntity::class.java)
        `when`(latestPackage.version).thenReturn(testVersion)
        `when`(latestPackage.releaseTs).thenReturn(testReleaseTs)
        `when`(packageRepository.findLatestByProjectId(testProjectId)).thenReturn(listOf(latestPackage))

        val result = uut.banByGroup(testGroupId, null)

        assertTrue(result)
        verify(blacklistRepository).addToBannedPackages(testGroupId, null, null)
        verify(blacklistRepository).removeBannedPackages(testGroupId, null)
        verify(blacklistRepository).removeBannedPackages()

        verify(projectRepository).findProjectsByPackages(testGroupId, null)
        verify(projectRepository).updateLatestVersion(testProjectId, testVersion, testReleaseTs)
    }

    @Test
    fun testNoProjectsUpdatedWhenNoLatestPackages() {
        `when`(projectRepository.findProjectsByPackages(testGroupId, testArtifactId)).thenReturn(setOf(testProjectId))
        `when`(packageRepository.findLatestByProjectId(testProjectId)).thenReturn(emptyList())

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(blacklistRepository).checkPackageExists(testGroupId, testArtifactId)
        verify(blacklistRepository).checkPackageBanned(testGroupId, testArtifactId)
        verify(blacklistRepository).addToBannedPackages(testGroupId, testArtifactId, null)
        verify(blacklistRepository).removeBannedPackages(testGroupId, testArtifactId)
        verify(blacklistRepository).removeBannedPackages()

        // Verify that the project repository was called to find projects by packages
        verify(projectRepository).findProjectsByPackages(testGroupId, testArtifactId)
        verifyNoMoreInteractions(projectRepository)
    }

    @Test
    fun testNoProjectsUpdatedWhenNoConnectedProjects() {
        `when`(projectRepository.findProjectsByPackages(testGroupId, testArtifactId)).thenReturn(emptySet())

        val result = uut.banPackage(testGroupId, testArtifactId, null)

        assertTrue(result)
        verify(blacklistRepository).checkPackageExists(testGroupId, testArtifactId)
        verify(blacklistRepository).checkPackageBanned(testGroupId, testArtifactId)
        verify(blacklistRepository).addToBannedPackages(testGroupId, testArtifactId, null)
        verify(blacklistRepository).removeBannedPackages(testGroupId, testArtifactId)
        verify(blacklistRepository).removeBannedPackages()

        // Verify that the project repository was called to only find projects by packages
        // and for nothing else
        verify(projectRepository).findProjectsByPackages(testGroupId, testArtifactId)
        verifyNoMoreInteractions(projectRepository)
    }
}
