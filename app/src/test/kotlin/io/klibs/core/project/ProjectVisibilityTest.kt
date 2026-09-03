package io.klibs.core.project

import BaseUnitWithDbLayerTest
import io.klibs.core.search.service.SearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A hidden project leaves every project read path and both search projections, while a direct package link
 * to one of its artifacts keeps working.
 */
@ActiveProfiles("test")
class ProjectVisibilityTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Autowired
    private lateinit var projectService: ProjectService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql(scripts = [SEED, HIDE])
    fun `project details and packages answer not found for a hidden project`() {
        listOf(
            "/project/Kotlin/kotlinx-atomicfu/details",
            "/project/$PROJECT_ID/details",
            "/project/Kotlin/kotlinx-atomicfu/packages"
        ).forEach { path ->
            mockMvc.get(path).andExpect { status { isNotFound() } }
        }
    }

    /**
     * These stay 200 for any project that exists, since a project legitimately without README content
     * (every androidx project with no bundled readme) has always answered with an empty body.
     * What matters is that no README content is served for a hidden project.
     */
    @Test
    @Sql(scripts = [SEED, HIDE])
    fun `no readme content is served for a hidden project`() {
        listOf(
            "/project/Kotlin/kotlinx-atomicfu/readme/markdown",
            "/project/Kotlin/kotlinx-atomicfu/readme/html"
        ).forEach { path ->
            val body = mockMvc.get(path)
                .andExpect { status { isOk() } }
                .andReturn().response.contentAsString

            assertEquals("", body, "$path served content for a hidden project")
        }
    }

    @Test
    @Sql(scripts = [SEED])
    fun `a visible project is served on its read paths`() {
        mockMvc.get("/project/Kotlin/kotlinx-atomicfu/details").andExpect { status { isOk() } }
        mockMvc.get("/project/Kotlin/kotlinx-atomicfu/packages").andExpect { status { isOk() } }
        assertEquals(1, projectIndexRows(), "the project should be in project_index")
        assertEquals(1, packageIndexRows(), "its packages should be in package_index")
    }

    @Test
    @Sql(scripts = [SEED, HIDE])
    fun `a hidden project is absent from both search projections`() {
        assertEquals(0, projectIndexRows(), "the project should have left project_index")
        assertEquals(0, packageIndexRows(), "its packages should have left package_index")
    }

    @Test
    @Sql(scripts = [SEED, HIDE])
    fun `project and package search return no hit for a hidden project`() {
        listOf("/search/projects?query=atomicfu", "/search/packages?query=atomicfu").forEach { path ->
            val body = mockMvc.get(path)
                .andExpect { status { isOk() } }
                .andReturn().response.contentAsString

            assertFalse(body.contains("atomicfu"), "$path still returns the hidden project: $body")
        }
    }

    @Test
    @Sql(scripts = [SEED, HIDE])
    fun `the sitemap omits a hidden project`() {
        // The /sitemap.xml endpoint serves a daily snapshot taken at startup, so the source is what to assert on.
        val projectNames = projectService.findAllForSitemap().map { it.projectName }

        assertFalse(projectNames.contains("kotlinx-atomicfu"), "the sitemap still lists the hidden project")
    }

    @Test
    @Sql(scripts = [SEED])
    fun `the sitemap lists a visible project`() {
        val projectNames = projectService.findAllForSitemap().map { it.projectName }

        assertTrue(projectNames.contains("kotlinx-atomicfu"), "the sitemap should list the visible project")
    }

    @Test
    @Sql(scripts = [SEED, HIDE, RESET_PACKAGE_CONFIGURATION])
    fun `a direct package link stays reachable for a hidden project`() {
        val details = mockMvc.get("/package/org.jetbrains.kotlinx/atomicfu/details")
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString

        assertTrue(details.contains("atomicfu"), "the package details should still be served: $details")
    }

    private fun projectIndexRows(): Int =
        jdbcClient.sql("SELECT count(*) FROM project_index WHERE project_id = :id")
            .param("id", PROJECT_ID)
            .query(Int::class.java)
            .single()

    private fun packageIndexRows(): Int =
        jdbcClient.sql("SELECT count(*) FROM package_index WHERE project_id = :id")
            .param("id", PROJECT_ID)
            .query(Int::class.java)
            .single()

    private companion object {
        const val SEED = "classpath:sql/ProjectControllerTest/seed.sql"
        const val HIDE = "classpath:sql/ProjectVisibilityTest/hide-project.sql"
        const val RESET_PACKAGE_CONFIGURATION = "classpath:sql/ProjectVisibilityTest/reset-package-configuration.sql"
        const val PROJECT_ID = 18
    }
}
