package io.klibs.core.project.controller

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.project.api.AllAllowedTagsResponse
import io.klibs.core.project.api.AllowedTagCreationRequest
import io.klibs.core.project.dto.AllowedTag
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
class AllowedTagsControllerTest : SmokeTestBase() {

    @Autowired
    private lateinit var allowedProjectTagsRepository: AllowedProjectTagsRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        allowedProjectTagsRepository.deleteAll()
    }

    @Test
    fun `check adding and removing tag`() {
        val tagName = "new-tag"
        val tag = AllowedTag(
            name = tagName,
            definition = "A new tag definition",
            positiveCues = listOf("pos1", "pos2"),
            negativesCues = listOf("neg1"),
            synonyms = listOf("syn1")
        )

        val creationRequest = AllowedTagCreationRequest(
            name = tag.name,
            definition = tag.definition,
            positiveCues = tag.positiveCues,
            negativesCues = tag.negativesCues,
            synonyms = tag.synonyms
        )

        // Create
        mockMvc.post("/tags/allowed") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(creationRequest)
        }.andExpect { status { isCreated() } }

        // Get all
        val allAllowedTagsNames = mockMvc.get("/tags/allowed").andExpect {
            status { isOk() }
        }.andReturn().let { result ->
            objectMapper.readValue(
                result.response.contentAsString,
                AllAllowedTagsResponse::class.java
            )
        }.tags.map { it.name }

        Assertions.assertTrue(allAllowedTagsNames.contains(tag.name))

        // Delete
        mockMvc.delete("/tags/allowed/$tagName").andExpect {
            status { isNoContent() }
        }

        // Get all
        val allAllowedTagsNamesAfterUpdate = mockMvc.get("/tags/allowed").andExpect {
            status { isOk() }
        }.andReturn().let { result ->
            objectMapper.readValue(
                result.response.contentAsString,
                AllAllowedTagsResponse::class.java
            )
        }.tags.map { it.name }

        Assertions.assertFalse(allAllowedTagsNamesAfterUpdate.contains(tag.name))
    }

    @Test
    fun `should return 400 when creating existing tag`() {
        val creationRequest = AllowedTagCreationRequest(name = "absolutely-new-tag", definition = null)

        mockMvc.post("/tags/allowed") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(creationRequest)
        }.andExpect { status { isCreated() } }

        mockMvc.post("/tags/allowed") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(creationRequest)
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 400 when creating tag with wong name`() {
        val tag = AllowedTagCreationRequest(name = "absolutely should not contain spaces", definition = null)
        mockMvc.post("/tags/allowed") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(tag)
        }.andExpect { status { isBadRequest() } }
    }
}
