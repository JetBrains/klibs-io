package io.klibs.core.readme

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import io.klibs.core.project.ANDROIDX_DEFAULT_DESCRIPTION
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.service.AndroidxReadmeFetcher
import io.klibs.core.readme.service.AndroidxReadmesUpdatingService
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.github.model.ReadmeFetchResult.Content
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AndroidxReadmesUpdatingServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: AndroidxReadmesUpdatingService

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @MockkBean
    private lateinit var androidxReadmeFetcher: AndroidxReadmeFetcher

    @MockkBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockkBean(relaxUnitFun = true)
    private lateinit var s3ReadmeService: S3ReadmeCRUDService

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadme stores fetched readme and clears default description`() {
        val project = findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        assertEquals(ANDROIDX_DEFAULT_DESCRIPTION, project.description)
        val expected = project.copy(description = null, minimizedReadme = MINIMIZED_README)
        every { androidxReadmeFetcher.getReadme("paging") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README

        uut.syncReadme(project)

        assertEquals(expected, project)
        assertEquals(expected, findProject(DEFAULT_DESCRIPTION_PROJECT_ID))
        verify(exactly = 1) {
            s3ReadmeService.writeReadmeFiles(
                DEFAULT_DESCRIPTION_PROJECT_ID,
                README,
                RENDERED_README,
                RENDERED_README
            )
        }
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadme keeps custom description when storing fetched readme`() {
        val project = findProject(CUSTOM_DESCRIPTION_PROJECT_ID)
        val expected = project.copy(minimizedReadme = MINIMIZED_README)
        every { androidxReadmeFetcher.getReadme("compose") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README

        uut.syncReadme(project)

        assertEquals(expected, project)
        assertEquals(expected, findProject(CUSTOM_DESCRIPTION_PROJECT_ID))
        verify(exactly = 1) {
            s3ReadmeService.writeReadmeFiles(
                CUSTOM_DESCRIPTION_PROJECT_ID,
                README,
                RENDERED_README,
                RENDERED_README
            )
        }
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadme leaves project untouched when readme content is not fetched`() {
        val project = findProject(CUSTOM_DESCRIPTION_PROJECT_ID)
        val expected = project.copy()
        every { androidxReadmeFetcher.getReadme("compose") } returns ReadmeFetchResult.Error(
            status = 200,
            message = "Unexpected content type: text/html"
        )

        uut.syncReadme(project)

        assertEquals(expected, project)
        assertEquals(expected, findProject(CUSTOM_DESCRIPTION_PROJECT_ID))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadme propagates readme fetching failure`() {
        val project = findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        val expected = project.copy()
        every { androidxReadmeFetcher.getReadme("paging") } throws IOException("Connection failure")

        val failure = assertFailsWith<IOException> { uut.syncReadme(project) }

        assertEquals("Connection failure", failure.message)
        assertEquals(expected, project)
        assertEquals(expected, findProject(DEFAULT_DESCRIPTION_PROJECT_ID))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadme propagates markdown rendering failure`() {
        val project = findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        val expected = project.copy()
        every { androidxReadmeFetcher.getReadme("paging") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns null

        val failure = assertFailsWith<IllegalStateException> { uut.syncReadme(project) }

        assertEquals("Problem rendering markdown for androidx.paging", failure.message)
        assertEquals(expected, project)
        assertEquals(expected, findProject(DEFAULT_DESCRIPTION_PROJECT_ID))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadme propagates readme storing failure`() {
        val project = findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        val expected = project.copy()
        every { androidxReadmeFetcher.getReadme("paging") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every {
            s3ReadmeService.writeReadmeFiles(DEFAULT_DESCRIPTION_PROJECT_ID, README, RENDERED_README, RENDERED_README)
        } throws IllegalStateException("S3 is unavailable")

        val failure = assertFailsWith<IllegalStateException> { uut.syncReadme(project) }

        assertEquals("S3 is unavailable", failure.message)
        assertEquals(expected, project)
        assertEquals(expected, findProject(DEFAULT_DESCRIPTION_PROJECT_ID))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadmes updates projects whose stored readme is missing`() {
        val paging = findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        every { androidxReadmeFetcher.getReadme("paging") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every { s3ReadmeService.readReadmeRaw(DEFAULT_DESCRIPTION_PROJECT_ID, null) } returns null

        uut.syncReadmes()

        assertEquals(
            paging.copy(description = null, minimizedReadme = MINIMIZED_README),
            findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        )
        verify(exactly = 1) {
            s3ReadmeService.writeReadmeFiles(
                DEFAULT_DESCRIPTION_PROJECT_ID,
                README,
                RENDERED_README,
                RENDERED_README
            )
        }
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadmes updates projects whose stored readme is outdated`() {
        val compose = findProject(CUSTOM_DESCRIPTION_PROJECT_ID)
        every { androidxReadmeFetcher.getReadme("compose") } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every { s3ReadmeService.readReadmeRaw(CUSTOM_DESCRIPTION_PROJECT_ID, null) } returns "# Outdated"

        uut.syncReadmes()

        assertEquals(compose.copy(minimizedReadme = MINIMIZED_README), findProject(CUSTOM_DESCRIPTION_PROJECT_ID))
        verify(exactly = 1) {
            s3ReadmeService.writeReadmeFiles(
                CUSTOM_DESCRIPTION_PROJECT_ID,
                README,
                RENDERED_README,
                RENDERED_README
            )
        }
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadmes skips projects whose stored readme is up to date`() {
        val compose = findProject(CUSTOM_DESCRIPTION_PROJECT_ID)
        val paging = findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        every { androidxReadmeFetcher.getReadme(any()) } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every { s3ReadmeService.readReadmeRaw(CUSTOM_DESCRIPTION_PROJECT_ID, null) } returns README
        every { s3ReadmeService.readReadmeRaw(DEFAULT_DESCRIPTION_PROJECT_ID, null) } returns "# Outdated"

        uut.syncReadmes()

        assertEquals(compose, findProject(CUSTOM_DESCRIPTION_PROJECT_ID))
        assertEquals(
            paging.copy(description = null, minimizedReadme = MINIMIZED_README),
            findProject(DEFAULT_DESCRIPTION_PROJECT_ID)
        )
        verify(exactly = 1) { s3ReadmeService.readReadmeRaw(CUSTOM_DESCRIPTION_PROJECT_ID, null) }
        verify(exactly = 1) { s3ReadmeService.readReadmeRaw(DEFAULT_DESCRIPTION_PROJECT_ID, null) }
        verify(exactly = 1) { s3ReadmeService.writeReadmeFiles(DEFAULT_DESCRIPTION_PROJECT_ID, any(), any(), any()) }
        verify(exactly = 1) { gitHubIntegration.markdownRenderRaw(README) }
        confirmVerified(s3ReadmeService)
        confirmVerified(gitHubIntegration)
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadmes skips androidx umbrella project`() {
        val umbrella = findProject(UMBRELLA_PROJECT_ID)
        every { androidxReadmeFetcher.getReadme(any()) } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every { s3ReadmeService.readReadmeRaw(any(), null) } returns null

        uut.syncReadmes()

        verify(exactly = 1) { androidxReadmeFetcher.getReadme("paging") }
        verify(exactly = 1) { androidxReadmeFetcher.getReadme("compose") }
        confirmVerified(androidxReadmeFetcher)
        assertEquals(umbrella, findProject(UMBRELLA_PROJECT_ID))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadmes skips projects of other owners`() {
        val foreign = findProject(FOREIGN_ID)
        every { androidxReadmeFetcher.getReadme(any()) } returns Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every { s3ReadmeService.readReadmeRaw(any(), null) } returns null

        uut.syncReadmes()

        verify(exactly = 1) { androidxReadmeFetcher.getReadme("paging") }
        verify(exactly = 1) { androidxReadmeFetcher.getReadme("compose") }
        confirmVerified(androidxReadmeFetcher)
        assertEquals(foreign, findProject(FOREIGN_ID))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/AndroidxReadmesUpdatingServiceTest/insert-androidx-projects.sql"])
    fun `syncReadmes continues with remaining projects when one fails`() {
        every { androidxReadmeFetcher.getReadme(any()) } throws
                IOException("Connection reset") andThen Content(README)
        every { gitHubIntegration.markdownRenderRaw(README) } returns RENDERED_README
        every { s3ReadmeService.readReadmeRaw(any(), null) } returns null

        uut.syncReadmes()

        verify(exactly = 2) { androidxReadmeFetcher.getReadme(any()) }
        verify(exactly = 1) { s3ReadmeService.writeReadmeFiles(any(), README, RENDERED_README, RENDERED_README) }
    }

    private fun findProject(id: Int): ProjectEntity = assertNotNull(projectRepository.findById(id))

    companion object {
        private const val UMBRELLA_PROJECT_ID = 7001
        private const val DEFAULT_DESCRIPTION_PROJECT_ID = 7002
        private const val CUSTOM_DESCRIPTION_PROJECT_ID = 7003
        private const val FOREIGN_ID = 7004

        private const val README = "# Readme"
        private const val RENDERED_README = "<h1>Readme</h1>"
        private const val MINIMIZED_README = "Readme"
    }
}
