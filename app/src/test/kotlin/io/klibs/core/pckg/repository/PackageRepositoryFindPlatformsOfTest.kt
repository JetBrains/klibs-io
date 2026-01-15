package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.model.PackagePlatform
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageRepositoryFindPlatformsOfTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Test
    @Sql(value = [
        "classpath:sql/PackageRepositoryFindPlatformsOfTest/seed-with-platforms.sql"
    ])
    fun `should find distinct platforms for latest packages per artifact in project`() {
        val projectId = 9001

        val platforms = packageRepository.findPlatformsOf(projectId)

        // Expect platforms from latest libA (JVM, NATIVE) and libB (JS). Older libA(JS) should be ignored.
        val expected = setOf(PackagePlatform.JVM, PackagePlatform.NATIVE, PackagePlatform.JS)
        val actual = platforms.toSet()

        assertEquals(expected, actual, "Should return distinct platforms across latest packages of the project")
    }

    @Test
    @Sql(value = [
        "classpath:sql/PackageRepositoryFindPlatformsOfTest/seed-empty-project.sql"
    ])
    fun `should return empty list when project has no packages`() {
        val projectId = 9100

        val platforms = packageRepository.findPlatformsOf(projectId)

        assertTrue(platforms.isEmpty(), "Expected empty list of platforms for project without packages")
    }
}
