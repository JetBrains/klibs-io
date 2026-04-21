package io.klibs.core.pckg.service

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.enums.VersionType
import io.klibs.core.pckg.repository.PackageIndexRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.Instant
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class PackageServiceTest {

    @Mock
    private lateinit var packageRepository: PackageRepository

    @Mock
    private lateinit var packageIndexRepository: PackageIndexRepository

    @Mock
    private lateinit var selfProvider: ObjectProvider<PackageService>

    private lateinit var uut: PackageService

    @BeforeEach
    fun setUp() {
        uut = PackageService(
            packageRepository,
            packageIndexRepository,
            selfProvider
        )
    }

    @Test
    fun `fillNullVersionTypes returns 0 when no packages have null version type`() {
        val pageable = PageRequest.ofSize(100)
        stubFindByVersionTypeIsNull(pageable, emptyList())

        val result = uut.fillNullVersionTypes(100)

        assertEquals(0, result)
        verify(packageRepository, never()).saveAll(anyList())
    }

    @Test
    fun `fillNullVersionTypes processes a batch and assigns correct version types`() {
        val pageable = PageRequest.ofSize(100)
        val stablePackage = createTestPackageEntity(id = 1L, version = "1.0.0")
        val preReleasePackage = createTestPackageEntity(id = 2L, version = "2.0.0-beta.1")
        val nonSemverPackage = createTestPackageEntity(id = 3L, version = "not-a-version")

        stubFindByVersionTypeIsNull(pageable, listOf(stablePackage, preReleasePackage, nonSemverPackage))
        stubSaveAllAndCapture { savedPackages ->
            assertEquals(3, savedPackages.size)
            assertEquals(VersionType.STABLE, savedPackages[0].versionType)
            assertEquals(VersionType.NON_STABLE, savedPackages[1].versionType)
            assertEquals(VersionType.NON_SEMVER, savedPackages[2].versionType)
        }

        val result = uut.fillNullVersionTypes(100)

        assertEquals(3, result)
        verify(packageRepository).saveAll(anyList())
    }

    @Test
    fun `fillNullVersionTypes respects batch size parameter`() {
        val pageable = PageRequest.ofSize(5)
        val packages = (1L..5L).map { createTestPackageEntity(id = it, version = "$it.0.0") }

        stubFindByVersionTypeIsNull(pageable, packages)
        stubSaveAllAndCapture { savedPackages ->
            assertEquals(5, savedPackages.size)
        }

        val result = uut.fillNullVersionTypes(5)

        assertEquals(5, result)
        verify(packageRepository).findByVersionTypeIsNull(pageable)
    }

    @Test
    fun `fillAllNullVersionTypes processes single batch then stops`() {
        val pageable = PageRequest.ofSize(100)
        val packages = listOf(createTestPackageEntity(id = 1L, version = "1.0.0"))

        whenever(selfProvider.getObject()).thenReturn(uut)
        whenever(packageRepository.findByVersionTypeIsNull(pageable))
            .thenReturn(packages)
            .thenReturn(emptyList())
        stubSaveAllAndCapture { }

        val result = uut.fillAllNullVersionTypes(100)

        assertEquals(1, result)
        verify(packageRepository, times(1)).findByVersionTypeIsNull(pageable)
    }

    @Test
    fun `fillAllNullVersionTypes uses default batch size`() {
        val pageable = PageRequest.ofSize(1000)
        whenever(selfProvider.getObject()).thenReturn(uut)
        stubFindByVersionTypeIsNull(pageable, emptyList())

        uut.fillAllNullVersionTypes()

        verify(packageRepository).findByVersionTypeIsNull(pageable)
    }

    private fun createTestPackageEntity(
        id: Long = 1L,
        version: String = "1.0.0",
        versionType: VersionType? = null
    ): PackageEntity {
        return PackageEntity(
            id = id,
            projectId = 100,
            repo = ScraperType.CENTRAL_SONATYPE,
            groupId = "io.klibs",
            artifactId = "test-package",
            version = version,
            releaseTs = Instant.now(),
            description = "A test package",
            url = "https://example.com",
            scmUrl = "https://github.com/example/test",
            buildTool = "gradle",
            buildToolVersion = "7.0.0",
            kotlinVersion = "1.5.0",
            developers = emptyList(),
            licenses = emptyList(),
            configuration = null,
            generatedDescription = false,
            versionType = versionType
        )
    }

    private fun stubFindByVersionTypeIsNull(pageable: Pageable, result: List<PackageEntity>) {
        whenever(packageRepository.findByVersionTypeIsNull(pageable)).thenReturn(result)
    }

    /**
     * Stubs saveAll to capture the saved entities and return them, then runs [verification] on the captured list.
     */
    private fun stubSaveAllAndCapture(verification: (List<PackageEntity>) -> Unit) {
        whenever(packageRepository.saveAll(anyList())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val saved = invocation.arguments[0] as List<PackageEntity>
            verification(saved)
            saved
        }
    }
}
