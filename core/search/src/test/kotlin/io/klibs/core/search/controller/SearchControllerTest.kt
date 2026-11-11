package io.klibs.core.search.controller

import io.klibs.core.search.service.SearchService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SearchControllerTest {
    private val searchService = mockk<SearchService>()
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(SearchController(searchService)).build()

    @BeforeEach
    fun setUp() {
        every { searchService.search(any(), any(), any(), any(), any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `search projects accepts an omitted body without content type`() {
        mockMvc.post("/search/projects")
            .andExpect {
                status { isOk() }
                content { string("[]") }
            }

        verifyDefaultSearch(page = 1, limit = 20)
    }

    @Test
    fun `search projects accepts an omitted body with json content type`() {
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { string("[]") }
        }

        verifyDefaultSearch(page = 1, limit = 20)
    }

    @Test
    fun `search projects uses pagination parameters when the body is omitted`() {
        mockMvc.post("/search/projects") {
            param("page", "3")
            param("limit", "7")
        }.andExpect {
            status { isOk() }
            content { string("[]") }
        }

        verifyDefaultSearch(page = 3, limit = 7)
    }

    private fun verifyDefaultSearch(page: Int, limit: Int) {
        verify(exactly = 1) {
            searchService.search(
                query = null,
                targetGroupFilters = emptyList(),
                ownerLogin = null,
                sort = SearchSort.RELEVANCY,
                markers = emptyList(),
                tags = emptyList(),
                page = page,
                limit = limit
            )
        }
    }
}