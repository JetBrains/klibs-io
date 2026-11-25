package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import io.klibs.core.project.ANDROIDX_DEFAULT_DESCRIPTION
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.service.AndroidxReadmeFetcher
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult.Content
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.io.IOException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AndroidxReadmeSyncListenerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var projectIndexingService: ProjectIndexingService

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockkBean
    private lateinit var androidxReadmeFetcher: AndroidxReadmeFetcher

    @MockkBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockkBean(relaxUnitFun = true)
    private lateinit var s3ReadmeService: S3ReadmeCRUDService

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmeSyncListenerTest/insert-androidx-repository.sql"])
    fun `readme of a new androidx project is stored after the saving transaction commits`() {
        every { androidxReadmeFetcher.getReadme("paging") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README

        val saved = projectIndexingService.save(pagingArtifact(), findAndroidxRepository())

        val stored = awaitSyncedProject(saved.idNotNull)
        assertEquals(MINIMIZED_README, stored.minimizedReadme)
        assertNull(stored.description)
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmeSyncListenerTest/insert-androidx-repository.sql"])
    fun `new androidx project is saved with the default description when readme fetching fails`() {
        every { androidxReadmeFetcher.getReadme("paging") } throws IOException("Connection failure")

        val saved = projectIndexingService.save(pagingArtifact(), findAndroidxRepository())

        verify(timeout = AWAIT_TIMEOUT_MS) { androidxReadmeFetcher.getReadme("paging") }
        val stored = findProject(saved.idNotNull)
        assertEquals(ANDROIDX_DEFAULT_DESCRIPTION, stored.description)
        assertNull(stored.minimizedReadme)
    }

    private fun pagingArtifact() = MavenArtifact(
        groupId = "androidx.paging",
        artifactId = "paging-common",
        version = "3.2.0",
        scraperType = ScraperType.GOOGLE_MAVEN,
        releasedAt = Instant.parse("2024-06-01T00:00:00Z"),
    )

    private fun findAndroidxRepository(): ScmRepositoryEntity =
        assertNotNull(scmRepositoryRepository.findById(ANDROIDX_REPOSITORY_ID))

    private fun findProject(id: Int): ProjectEntity = assertNotNull(projectRepository.findById(id))

    private fun awaitSyncedProject(projectId: Int): ProjectEntity {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        var project = findProject(projectId)
        while (project.minimizedReadme != MINIMIZED_README && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            project = findProject(projectId)
        }
        return project
    }

    companion object {
        private const val ANDROIDX_REPOSITORY_ID = 601
        private const val README = "# Readme"
        private const val RENDERED_README = "<h1>Readme</h1>"
        private const val MINIMIZED_README = "Readme"
        private const val AWAIT_TIMEOUT_MS = 10_000L
    }
}
