package io.klibs.core.pckg.controller

import BaseUnitWithDbLayerTest
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.api.PackageStatusResponse
import io.klibs.core.pckg.enums.PackageProcessingStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals

@ActiveProfiles("test")
class PackageControllerStatusTest : BaseUnitWithDbLayerTest(){

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var mockMvc : MockMvc

    @Test
    @Sql(scripts = ["classpath:sql/PackageControllerTest/seed-project-with-packages.sql"])
    fun `should return INDEXED for an indexed package`() {
        val result = mockMvc.get("/package/org.example/libA/2.0.0/status")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<PackageStatusResponse>() {}
        )

        assertEquals("org.example", response.groupId)
        assertEquals("libA", response.artifactId)
        assertEquals("2.0.0", response.version)
        assertEquals(PackageProcessingStatus.INDEXED, response.status)
        assertEquals(
            "This package has been indexed and is available on klibs.io.",
            response.statusDescription
        )
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageControllerTest/seed-package-index-request.sql"])
    fun `should return QUEUED for a request with a next attempt`() {
        val result = mockMvc.get("/package/org.queued/libA/1.0.0/status")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<PackageStatusResponse>() {}
        )

        assertEquals(PackageProcessingStatus.QUEUED, response.status)
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageControllerTest/seed-package-index-request.sql"])
    fun `should return FAILED for a request without a next attempt`() {
        val result = mockMvc.get("/package/org.failed/libA/1.0.0/status")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<PackageStatusResponse>() {}
        )

        assertEquals(PackageProcessingStatus.FAILED, response.status)
    }

    @Test
    fun `should return UNKNOWN for an unknown package`() {
        val result = mockMvc.get("/package/org.unknown/libA/1.0.0/status")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<PackageStatusResponse>() {}
        )

        assertEquals(PackageProcessingStatus.UNKNOWN, response.status)
    }
}
