package io.klibs.core.search

import BaseUnitWithDbLayerTest
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.api.SearchPackageResultDTO
import io.klibs.core.search.dto.api.SearchPackagesRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.post
import kotlin.test.assertTrue

class PackageSearchUnknownTargetControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc

    @BeforeEach
    fun refreshViews() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageSearchUnknownTargetControllerTest/insert-package-with-unknown-target.sql"])
    @DisplayName("Should not error and return Unknown target group when package has unknown target")
    fun shouldReturnUnknownTargetGroupForUnknownTarget() {
        val mvcResult = mockMvc.post("/search/packages") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SearchPackagesRequest(query = "unknown-target-lib"))
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            mvcResult.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        val pkg = foundPackages.firstOrNull { it.artifactId == "unknown-target-lib" }
        assertTrue(pkg != null, "Response should contain the inserted package 'unknown-target-lib'")
        assertTrue(
            pkg.targets.containsKey(TargetGroup.Unknown),
            "Targets should contain Unknown target group when unknown targets are present"
        )
        assertTrue(
            pkg.targets.containsKey(TargetGroup.JVM),
            "Targets should contain normal target"
        )
    }
}
