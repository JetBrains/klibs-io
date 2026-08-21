package io.klibs.core.search.eval

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import io.klibs.core.search.dto.api.SearchProjectsRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

private val mapper = jacksonObjectMapper()

fun MockMvc.searchProjects(case: EvalCase): List<SearchResult> {
    // Each group becomes its own single-group map, i.e. AND between groups (see EvalCase.targetFilters).
    val body = SearchProjectsRequest(
        query = case.query,
        targetGroupFilters = case.targetFilters.map { mapOf(it.key to it.value) },
    )
    val requestBuilder = post("/search/projects")
        .param("limit", "20")
        .param("page", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))
    val responseBody = perform(requestBuilder).andReturn().response.contentAsString
    return mapper.readValue<List<SearchProjectResultDTO>>(responseBody).map { dto ->
        SearchResult(
            key = "${dto.ownerLogin}/${dto.name}".lowercase(),
            platforms = dto.platforms.map { it.lowercase() }.toSet(),
        )
    }
}
