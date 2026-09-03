package io.klibs.core.project

import BaseUnitWithDbLayerTest
import io.klibs.core.project.enums.HideOrigin
import io.klibs.core.project.repository.ProjectHiddenRepository
import io.klibs.core.search.service.SearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ActiveProfiles("test")
class ProjectVisibilityControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var projectHiddenRepository: ProjectHiddenRepository

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql(scripts = [SEED])
    fun `hide stops serving the project and unhide brings it back`() {
        mockMvc.get(DETAILS).andExpect { status { isOk() } }

        mockMvc.post("$HIDE&reason=maintainer-request").andExpect { status { isOk() } }

        val hidden = assertNotNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
        assertEquals(HideOrigin.MANUAL, hidden.origin)
        assertEquals("maintainer-request", hidden.reason)
        searchService.refreshSearchViews()
        mockMvc.get(DETAILS).andExpect { status { isNotFound() } }

        mockMvc.post(UNHIDE).andExpect { status { isOk() } }

        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
        searchService.refreshSearchViews()
        mockMvc.get(DETAILS).andExpect { status { isOk() } }
    }

    @Test
    @Sql(scripts = [SEED, AUTO_HIDE])
    fun `unhide also clears a hide made by an unreachable repository`() {
        assertEquals(HideOrigin.AUTO, projectHiddenRepository.findByProjectId(PROJECT_ID)?.origin)

        mockMvc.post(UNHIDE).andExpect { status { isOk() } }

        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
    }

    @Test
    @Sql(scripts = [SEED])
    fun `repeating either call is not an error`() {
        repeat(2) { mockMvc.post(HIDE).andExpect { status { isOk() } } }
        assertNotNull(projectHiddenRepository.findByProjectId(PROJECT_ID))

        repeat(2) { mockMvc.post(UNHIDE).andExpect { status { isOk() } } }
        assertNull(projectHiddenRepository.findByProjectId(PROJECT_ID))
    }

    @Test
    @Sql(scripts = [SEED])
    fun `both endpoints answer not found for an unknown project`() {
        listOf(
            "/project-visibility/hide?ownerLogin=Kotlin&projectName=no-such-project",
            "/project-visibility/unhide?ownerLogin=Kotlin&projectName=no-such-project"
        ).forEach { path ->
            mockMvc.post(path).andExpect { status { isNotFound() } }
        }
    }

    private companion object {
        const val SEED = "classpath:sql/ProjectControllerTest/seed.sql"
        const val AUTO_HIDE = "classpath:sql/ProjectVisibilityTest/hide-project.sql"
        const val PROJECT_ID = 18
        const val DETAILS = "/project/Kotlin/kotlinx-atomicfu/details"
        const val HIDE = "/project-visibility/hide?ownerLogin=Kotlin&projectName=kotlinx-atomicfu"
        const val UNHIDE = "/project-visibility/unhide?ownerLogin=Kotlin&projectName=kotlinx-atomicfu"
    }
}
