package io.klibs.core.search.eval

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

private val mapper = jacksonObjectMapper()

fun MockMvc.searchProjects(case: EvalCase): List<SearchResult> {
    val builder = get("/search/projects").param("query", case.query).param("limit", "20").param("page", "1")
    case.platforms.forEach { builder.param("platforms", it) }
    val body = perform(builder).andReturn().response.contentAsString
    return mapper.readTree(body).map { n ->
        SearchResult(
            key = "${n.path("ownerLogin").asText("")}/${n.path("name").asText("")}".lowercase(),
            platforms = n.path("platforms").map { it.asText().lowercase() }.toSet(),
        )
    }
}
