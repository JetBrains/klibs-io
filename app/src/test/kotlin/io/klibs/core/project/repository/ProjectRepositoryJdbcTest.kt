package io.klibs.core.project.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.project.ProjectEntity
import java.time.Instant
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Transactional
@Sql("classpath:sql/ScmRepositoryRepositoryDbTest/seed-repos.sql")
class ProjectRepositoryJdbcTest : BaseUnitWithDbLayerTest() {
    @Autowired
    private lateinit var repository: ProjectRepository

    @Test
    fun `invalid README is omitted on insert and transaction remains usable`() {
        val project = repository.insert(project("# README\u0000broken"))

        assertNull(project.minimizedReadme)
        assertNull(repository.findById(project.idNotNull)?.minimizedReadme)
        repository.updateDescription(project.idNotNull, "Package metadata saved")
        assertEquals("Package metadata saved", repository.findById(project.idNotNull)?.description)
    }

    @Test
    fun `invalid README update preserves existing text and transaction remains usable`() {
        val project = repository.insert(project("# Existing README"))

        repository.updateMinimizedReadme(project.idNotNull, "# Broken\u0000 README")
        repository.updateDescription(project.idNotNull, "Updated metadata")

        val saved = requireNotNull(repository.findById(project.idNotNull))
        assertEquals("# Existing README", saved.minimizedReadme)
        assertEquals("Updated metadata", saved.description)
    }

    @Test
    fun `valid and null READMEs can be saved`() {
        val project = repository.insert(project("# Hello 世界"))
        assertEquals("# Hello 世界", repository.findById(project.idNotNull)?.minimizedReadme)

        repository.updateMinimizedReadme(project.idNotNull, "Updated Привет")
        assertEquals("Updated Привет", repository.findById(project.idNotNull)?.minimizedReadme)

        repository.updateMinimizedReadme(project.idNotNull, null)
        assertNull(repository.findById(project.idNotNull)?.minimizedReadme)
    }

    private fun project(readme: String?) = ProjectEntity(
        id = null,
        scmRepoId = 800010,
        ownerId = 800001,
        name = "repo-a",
        description = null,
        minimizedReadme = readme,
        latestVersion = "1.0.0",
        latestVersionTs = Instant.parse("2026-09-14T00:00:00Z"),
    )
}
