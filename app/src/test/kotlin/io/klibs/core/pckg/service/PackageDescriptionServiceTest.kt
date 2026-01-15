package io.klibs.core.pckg.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.ai.PackageDescriptionGenerator
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageDescriptionServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var packageDescriptionService: PackageDescriptionService

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Autowired
    private lateinit var packageService: PackageService

    @MockBean
    private lateinit var packageDescriptionGenerator: PackageDescriptionGenerator

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionServiceTest/insert-package-with-specific-version.sql"])
    fun `should generate description for a specific package with all parameters`() {
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "1.0.0"
        val packageName = "$groupId:$artifactId"
        val expectedDescription = "This is a test library for demonstration purposes."

        val packageEntity = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        assertNotNull(packageEntity, "Package entity should exist in the database")
        assertEquals("Old description", packageEntity.description, "Package should have the initial description")

        `when`(
            packageDescriptionGenerator.generatePackageDescription(
                packageName,
                groupId,
                artifactId,
                version
            )
        ).thenReturn(expectedDescription)

        val result = packageDescriptionService.generateDescription(groupId, artifactId, version)

        assertEquals(expectedDescription, result)
    }

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionServiceTest/insert-package-with-groupid-artifactid.sql"])
    fun `should generate description for a package with groupId and artifactId but no version`() {
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "2.0.0" // Latest version
        val packageName = "$groupId:$artifactId"
        val expectedDescription = "This is a description for the org.example:test-library package."

        val packageEntity = packageRepository.findFirstByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId, artifactId)
        assertNotNull(packageEntity, "Package entity should exist in the database")
        assertEquals(version, packageEntity.version, "Package should have the latest version")
        assertEquals("Old description 2", packageEntity.description, "Package should have the initial description")

        `when`(
            packageDescriptionGenerator.generatePackageDescription(
                packageName,
                groupId,
                artifactId,
                version
            )
        ).thenReturn(expectedDescription)

        val result = packageDescriptionService.generateDescription(groupId, artifactId)

        assertEquals(expectedDescription, result)
    }

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionServiceTest/insert-packages-with-same-groupid.sql"])
    fun `should generate description for a package with only groupId`() {
        val groupId = "org.example"
        val artifactId1 = "test-library"
        val artifactId2 = "test-utils"
        val version = "1.0.0"
        val packageName1 = "$groupId:$artifactId1"
        val packageName2 = "$groupId:$artifactId2"
        val expectedDescription = "This is a description for the org.example group."

        val packages = packageRepository.findLatestByGroupId(groupId)
        assertEquals(2, packages.size, "Should find 2 packages with the same groupId")

        val package1 = packages.find { it.artifactId == artifactId1 }
        assertNotNull(package1, "Package with artifactId $artifactId1 should exist")
        assertEquals("Old description 1", package1.description, "Package should have the initial description")

        val package2 = packages.find { it.artifactId == artifactId2 }
        assertNotNull(package2, "Package with artifactId $artifactId2 should exist")
        assertEquals("Old description 2", package2.description, "Package should have the initial description")

        `when`(
            packageDescriptionGenerator.generatePackageDescription(
                packageName1,
                groupId,
                artifactId1,
                version
            )
        ).thenReturn(expectedDescription)

        `when`(
            packageDescriptionGenerator.generatePackageDescription(
                packageName2,
                groupId,
                artifactId2,
                version
            )
        ).thenReturn(expectedDescription)

        val result = packageDescriptionService.generateDescription(groupId)

        assertTrue(result.contains("Updated descriptions for 2 packages with groupId=$groupId"))
        assertTrue(result.contains("$groupId:$artifactId1:$version"))
        assertTrue(result.contains("$groupId:$artifactId2:$version"))
    }

    @Test
    fun `should throw exception when no packages are found`() {
        val groupId = "org.nonexistent"
        val artifactId = "nonexistent-library"
        val version = "1.0.0"

        val packageWithVersion = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        assertEquals(null, packageWithVersion, "No package should exist with the specified parameters")

        val packageWithoutVersion =
            packageRepository.findFirstByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId, artifactId)
        assertEquals(null, packageWithoutVersion, "No package should exist with the specified groupId and artifactId")

        val packagesWithGroupId = packageRepository.findLatestByGroupId(groupId)
        assertTrue(packagesWithGroupId.isEmpty(), "No packages should exist with the specified groupId")

        assertThrows<IllegalArgumentException> {
            packageDescriptionService.generateDescription(groupId, artifactId, version)
        }

        assertThrows<IllegalArgumentException> {
            packageDescriptionService.generateDescription(groupId, artifactId)
        }

        assertThrows<IllegalArgumentException> {
            packageDescriptionService.generateDescription(groupId)
        }
    }

    @Test
    @Sql("/sql/PackageDescriptionServiceTest/insert-packages-with-duplicate-descriptions.sql")
    fun `should generate descriptions for packages`() {
        val originalDescriptions = mutableMapOf<Long, String>()
        val packageIds = listOf(9001L, 9002L, 9003L, 9004L, 9005L, 9006L)

        packageIds.forEach { id ->
            val packageEntity = packageRepository.findById(id).orElse(null)
            if (packageEntity != null) {
                originalDescriptions[id] = packageEntity.description ?: ""
            }
        }

        val duplicateDescriptions = packageRepository.findDuplicateDescriptions(10)

        assertTrue(duplicateDescriptions.isNotEmpty(), "Should have found duplicate descriptions")

        println("Found duplicate descriptions: $duplicateDescriptions")

        packageIds.forEach { id ->
            val packageEntity = packageRepository.findById(id).orElse(null)
            if (packageEntity != null) {
                `when`(
                    packageDescriptionGenerator.generatePackageDescription(
                        packageEntity.name,
                        packageEntity.groupId,
                        packageEntity.artifactId,
                        packageEntity.version
                    )
                ).thenReturn("Placeholder description for ${packageEntity.name} ${packageEntity.version}")
            }
        }

        packageDescriptionService.generateUniqueDescriptions()

        var updatedCount = 0

        packageIds.forEach { id ->
            val packageEntity = packageRepository.findById(id).orElse(null)
            if (packageEntity != null) {
                val newDescription = packageEntity.description

                if (newDescription != originalDescriptions[id]) {
                    updatedCount++

                    assertTrue(
                        newDescription!!.contains("Placeholder description for"),
                        "Description should contain the placeholder text"
                    )
                }
            }
        }

        assertTrue(updatedCount > 0, "Should have updated at least one description")
    }
}
