package io.klibs.core.pckg.entity

import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class PackageEntityTest {

    @Test
    fun `deepCopy should create a new instance with reattached targets`() {
        // Create a package entity with targets
        val originalPackage = createTestPackageEntity()
        val target1 = PackageTargetEntity(
            platform = PackagePlatform.JVM,
            target = "1.8"
        )
        val target2 = PackageTargetEntity(
            platform = PackagePlatform.JS,
            target = "ir"
        )

        originalPackage.addTarget(target1)
        originalPackage.addTarget(target2)

        // Perform deep copy
        val copiedPackage = originalPackage.deepCopy()

        // Verify the copy is not the same instance
        assertNotSame(originalPackage, copiedPackage)

        // Verify all properties are copied correctly
        assertEquals(originalPackage.id, copiedPackage.id)
        assertEquals(originalPackage.projectId, copiedPackage.projectId)
        assertEquals(originalPackage.repo, copiedPackage.repo)
        assertEquals(originalPackage.groupId, copiedPackage.groupId)
        assertEquals(originalPackage.artifactId, copiedPackage.artifactId)
        assertEquals(originalPackage.version, copiedPackage.version)
        assertEquals(originalPackage.releaseTs, copiedPackage.releaseTs)
        assertEquals(originalPackage.name, copiedPackage.name)
        assertEquals(originalPackage.description, copiedPackage.description)
        assertEquals(originalPackage.url, copiedPackage.url)
        assertEquals(originalPackage.scmUrl, copiedPackage.scmUrl)
        assertEquals(originalPackage.buildTool, copiedPackage.buildTool)
        assertEquals(originalPackage.buildToolVersion, copiedPackage.buildToolVersion)
        assertEquals(originalPackage.kotlinVersion, copiedPackage.kotlinVersion)
        assertEquals(originalPackage.developers, copiedPackage.developers)
        assertEquals(originalPackage.licenses, copiedPackage.licenses)
        assertEquals(originalPackage.configuration, copiedPackage.configuration)
        assertEquals(originalPackage.generatedDescription, copiedPackage.generatedDescription)

        // Verify targets are copied correctly
        assertEquals(originalPackage.targets.size, copiedPackage.targets.size)

        // Verify targets in the copy are not the same instances as in the original
        for (i in originalPackage.targets.indices) {
            val originalTarget = originalPackage.targets[i]
            val copiedTarget = copiedPackage.targets[i]

            assertNotSame(originalTarget, copiedTarget)

            // Verify target properties are copied correctly
            assertEquals(originalTarget.platform, copiedTarget.platform)
            assertEquals(originalTarget.target, copiedTarget.target)

            // Verify targets in the copy reference the copy, not the original
            assertSame(copiedPackage, copiedTarget.packageEntity)
            assertNotSame(originalPackage, copiedTarget.packageEntity)
        }
    }

    @Test
    fun `deepCopy should allow changing specific properties`() {
        // Create a package entity with targets
        val originalPackage = createTestPackageEntity()
        val target1 = PackageTargetEntity(
            platform = PackagePlatform.JVM,
            target = "1.8"
        )
        originalPackage.addTarget(target1)

        // New values for properties we want to change
        val newDescription = "Updated description"
        val newVersion = "2.0.0"
        val newGeneratedDescription = true

        // Perform deep copy with specific properties changed
        val copiedPackage = originalPackage.deepCopy(
            description = newDescription,
            version = newVersion,
            generatedDescription = newGeneratedDescription
        )

        // Verify changed properties
        assertEquals(newDescription, copiedPackage.description)
        assertEquals(newVersion, copiedPackage.version)
        assertEquals(newGeneratedDescription, copiedPackage.generatedDescription)

        // Verify unchanged properties remain the same
        assertEquals(originalPackage.id, copiedPackage.id)
        assertEquals(originalPackage.projectId, copiedPackage.projectId)
        assertEquals(originalPackage.repo, copiedPackage.repo)
        assertEquals(originalPackage.groupId, copiedPackage.groupId)
        assertEquals(originalPackage.artifactId, copiedPackage.artifactId)
        assertEquals(originalPackage.releaseTs, copiedPackage.releaseTs)
        assertEquals(originalPackage.name, copiedPackage.name)
        assertEquals(originalPackage.url, copiedPackage.url)
        assertEquals(originalPackage.scmUrl, copiedPackage.scmUrl)
        assertEquals(originalPackage.buildTool, copiedPackage.buildTool)
        assertEquals(originalPackage.buildToolVersion, copiedPackage.buildToolVersion)
        assertEquals(originalPackage.kotlinVersion, copiedPackage.kotlinVersion)
        assertEquals(originalPackage.developers, copiedPackage.developers)
        assertEquals(originalPackage.licenses, copiedPackage.licenses)
        assertEquals(originalPackage.configuration, copiedPackage.configuration)

        // Verify targets are still copied correctly
        assertEquals(originalPackage.targets.size, copiedPackage.targets.size)
        assertNotSame(originalPackage.targets[0], copiedPackage.targets[0])
        assertEquals(originalPackage.targets[0].platform, copiedPackage.targets[0].platform)
        assertEquals(originalPackage.targets[0].target, copiedPackage.targets[0].target)
        assertSame(copiedPackage, copiedPackage.targets[0].packageEntity)
    }

    private fun createTestPackageEntity(): PackageEntity {
        val projectSettings = Configuration.ProjectSettings(
            isHmppEnabled = true,
            isCompatibilityMetadataVariantEnabled = false
        )

        val jvmPlatform = Configuration.JvmPlatform(
            jvmTarget = "1.8",
            withJavaEnabled = true
        )

        val configuration = Configuration(
            projectSettings = projectSettings,
            jvmPlatform = jvmPlatform
        )

        return PackageEntity(
            id = 1L,
            projectId = 100,
            repo = ScraperType.CENTRAL_SONATYPE,
            groupId = "io.klibs",
            artifactId = "test-package",
            version = "1.0.0",
            releaseTs = Instant.now(),
            name = "Test Package",
            description = "A test package for unit testing",
            url = "https://example.com/test-package",
            scmUrl = "https://github.com/example/test-package",
            buildTool = "gradle",
            buildToolVersion = "7.0.0",
            kotlinVersion = "1.5.0",
            developers = listOf(PackageDeveloper("Test Developer", "https://example.com/developer")),
            licenses = listOf(PackageLicense("MIT", "https://opensource.org/licenses/MIT")),
            configuration = configuration,
            generatedDescription = false
        )
    }
}
