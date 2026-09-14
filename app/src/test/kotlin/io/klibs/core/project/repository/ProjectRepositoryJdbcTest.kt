package io.klibs.core.project.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.project.ProjectEntity
import java.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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

    @ParameterizedTest
    @ValueSource(strings = ["", "# Existing README"])
    fun `empty README can be persisted and replaces previous content`(initialReadme: String) {
        val project = repository.insert(project(initialReadme))
        assertEquals(initialReadme, project.minimizedReadme)
        assertEquals(initialReadme, repository.findById(project.idNotNull)?.minimizedReadme)

        repository.updateMinimizedReadme(project.idNotNull, "")
        repository.updateDescription(project.idNotNull, "Updated metadata")

        val saved = requireNotNull(repository.findById(project.idNotNull))
        assertEquals("", saved.minimizedReadme)
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
