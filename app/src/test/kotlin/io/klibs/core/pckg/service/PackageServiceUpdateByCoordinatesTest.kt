package io.klibs.core.pckg.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.dto.PackageDTO
import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageServiceUpdateByCoordinatesTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var packageService: PackageService

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Test
    fun `updateByCoordinates returns null when package not found`() {
        val dto = sampleDto(
            groupId = "io.klibs",
            artifactId = "missing",
            version = "1.0.0",
            targets = listOf(PackageTarget(PackagePlatform.JVM, "1.8"))
        )

        val result = packageService.updateByCoordinates(dto)

        assertNull(result)
    }

    @Test
    @Sql("/sql/PackageServiceUpdateByCoordinatesTest/insert-existing-package-with-targets.sql")
    @Transactional
    fun `updateByCoordinates reuses existing targets and creates new ones`() {
        val groupId = "io.klibs"
        val artifactId = "sample"
        val version = "1.0.0"

        val before = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        assertNotNull(before)
        // Initially we inserted two targets: JVM:1.8 and JS:ir
        assertEquals(2, before.targets.size)

        val dto = sampleDto(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            targets = listOf(
                PackageTarget(PackagePlatform.JVM, "1.8"), // should be reused
                PackageTarget(PackagePlatform.NATIVE, null)   // should be created
            )
        )

        val result = packageService.updateByCoordinates(dto)
        assertNotNull(result)

        val after = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        assertNotNull(after)

        assertEquals(2, after.targets.size)
        val targetsSet = after.targets.map { it.platform to it.target }.toSet()
        assertTrue(targetsSet.contains(PackagePlatform.JVM to "1.8"))
        assertTrue(targetsSet.contains(PackagePlatform.NATIVE to null))

        val resultTargets = result.targets.toSet()
        assertTrue(resultTargets.contains(PackageTarget(PackagePlatform.JVM, "1.8")))
        assertTrue(resultTargets.contains(PackageTarget(PackagePlatform.NATIVE, null)))
        assertEquals(2, resultTargets.size)
    }

    private fun sampleDto(
        groupId: String,
        artifactId: String,
        version: String,
        targets: List<PackageTarget>
    ): PackageDTO = PackageDTO(
        id = null,
        projectId = 8101,
        repo = ScraperType.SEARCH_MAVEN,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        releaseTs = Instant.now(),
        name = "$groupId:$artifactId",
        description = "Updated desc",
        url = "https://example.com/$artifactId",
        scmUrl = null,
        buildTool = "gradle",
        buildToolVersion = "8.0",
        kotlinVersion = "2.0.0",
        developers = listOf(PackageDeveloper("dev", null)),
        licenses = listOf(PackageLicense("Apache-2.0", null)),
        configuration = Configuration(
            projectSettings = Configuration.ProjectSettings(true, false),
            jvmPlatform = Configuration.JvmPlatform("1.8", true)
        ),
        generatedDescription = false,
        targets = targets
    )
}
