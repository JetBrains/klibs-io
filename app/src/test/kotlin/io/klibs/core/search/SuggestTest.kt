package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.search.suggest.SuggestService
import io.klibs.core.search.suggest.WordSuggestionDTO
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals

@ActiveProfiles("test")
class SuggestTest: SmokeTestBase() {

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var suggestService: SuggestService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
        suggestService.refreshKeywordIndex()
    }

    @Test
    fun `should return suggestions matching the query`() {
        // Arrange
        val query = "kot"

        val expectedList = listOf("anthropic-sdk-kotlin","aws-sdk-kotlin","Kotlin","kotlin-wrappers","kotlinx-atomicfu")

        // Act & Assert
        mockMvc.get("/search/suggest") {
            param("query", query)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$.keywords", hasSize<Int>(greaterThan(0)))
                jsonPath("$.keywords", `is`(expectedList))
            }
        }
    }

    @Test
    fun `should return empty list when no suggestions match`() {
        // Arrange
        val query = "XYZ"

        // Act & Assert
        val result = mockMvc.get("/search/suggest") {
            param("query", query)
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
        }.andReturn()
        val wordSuggestionDTO: WordSuggestionDTO = objectMapper.readValue(result.response.contentAsString, WordSuggestionDTO::class.java)
        assertEquals(0, wordSuggestionDTO.keywords.size)
    }

    @Test
    fun `should return empty list when query parameter is missing`() {
        val result = mockMvc.get("/search/suggest")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val wordSuggestionDTO: WordSuggestionDTO = objectMapper.readValue(result.response.contentAsString, WordSuggestionDTO::class.java)
        assertEquals(0, wordSuggestionDTO.keywords.size)
    }
}
