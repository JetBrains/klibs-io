package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.search.SearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageIndexRepositoryTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var packageIndexRepository: PackageIndexRepository

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql("classpath:sql/PackageIndexRepositoryTest/seed-project-with-packages.sql")
    fun `should return latest packages by project id`() {
        val projectId = 19001

        val rows = packageIndexRepository.findByProjectId(projectId)

        assertEquals(2, rows.size)
        assertTrue(rows.all { it.projectId == projectId })

        val expectedVersions = setOf("2.0.0", "3.1.4")
        val actualVersions = rows.map {
            it.latestVersion
        }.toSet()

        assertEquals(expectedVersions, actualVersions, "Should return newest versions of packages")
    }

    @Test
    @Sql("classpath:sql/PackageIndexRepositoryTest/seed-project-with-packages.sql")
    fun `should return empty list when project has no packages`() {
        val projectId = 19100

        val rows = packageIndexRepository.findByProjectId(projectId)

        assertTrue(rows.isEmpty(), "Expected empty list of packages for project without packages")
    }

    @Test
    @Sql("classpath:sql/PackageIndexRepositoryTest/seed-project-with-packages.sql")
    fun `should return latest packages by group id`() {
        val groupId = "org.example"

        val rows = packageIndexRepository.findByIdGroupId(groupId)

        assertEquals(2, rows.size)
        assertTrue(rows.all { it.id.groupId == groupId })

        val expectedVersions = setOf("2.0.0", "3.1.4")
        val actualVersions = rows.map {
            it.latestVersion
        }.toSet()

        assertEquals(expectedVersions, actualVersions, "Should return newest versions of packages")
    }
}