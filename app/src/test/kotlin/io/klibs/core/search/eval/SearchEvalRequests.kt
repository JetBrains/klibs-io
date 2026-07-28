package io.klibs.core.search.eval

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

private val mapper = jacksonObjectMapper()

fun MockMvc.searchProjects(case: EvalCase): List<SearchResult> {
    val requestBuilder = get("/search/projects").param("query", case.query).param("limit", "20").param("page", "1")
    case.platforms.forEach { requestBuilder.param("platforms", it) }
    val responseBody = perform(requestBuilder).andReturn().response.contentAsString
    return mapper.readValue<List<SearchProjectResultDTO>>(responseBody).map { dto ->
        SearchResult(
            key = "${dto.ownerLogin}/${dto.name}".lowercase(),
            platforms = dto.platforms.map { it.lowercase() }.toSet(),
        )
    }
}
