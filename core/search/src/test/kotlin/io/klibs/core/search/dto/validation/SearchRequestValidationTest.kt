package io.klibs.core.search.dto.validation

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.api.SearchPackagesRequest
import io.klibs.core.search.dto.api.SearchProjectsRequest
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKERS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_OWNER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_QUERY_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAGS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAG_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TARGET_GROUP_FILTERS
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchRequestValidationTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `accepts project request at all limits`() {
        val request = SearchProjectsRequest(
            query = "y".repeat(MAX_QUERY_LENGTH),
            targetGroupFilters = List(MAX_TARGET_GROUP_FILTERS) { mapOf(TargetGroup.JVM to emptySet()) },
            owner = "o".repeat(MAX_OWNER_LENGTH),
            tags = List(MAX_TAGS) { "t".repeat(MAX_TAG_LENGTH) },
            markers = List(MAX_MARKERS) { "m".repeat(MAX_MARKER_LENGTH) },
        )
        assertTrue(validator.validate(request).isEmpty())
    }

    @Test
    fun `rejects project request over limits`() {
        val request = SearchProjectsRequest(
            query = "y".repeat(MAX_QUERY_LENGTH + 1),
            targetGroupFilters = List(MAX_TARGET_GROUP_FILTERS + 1) { mapOf(TargetGroup.JVM to emptySet()) },
            owner = "o".repeat(MAX_OWNER_LENGTH + 1),
            tags = List(MAX_TAGS + 1) { "tag$it" },
            markers = List(MAX_MARKERS + 1) { "marker$it" },
        )
        assertEquals(setOf("query", "targetGroupFilters", "owner", "tags", "markers"), violatedProperties(request))
    }

    @Test
    fun `rejects a single tag over the length limit`() {
        val request = SearchProjectsRequest(tags = listOf("t".repeat(MAX_TAG_LENGTH + 1)))
        assertEquals(setOf("tags[0].<list element>"), violatedProperties(request))
    }

    @Test
    fun `rejects a single marker over the length limit`() {
        val request = SearchProjectsRequest(markers = listOf("GRANT_WINNER_2024", "m".repeat(MAX_MARKER_LENGTH + 1)))
        assertEquals(setOf("markers[1].<list element>"), violatedProperties(request))
    }

    @Test
    fun `rejects package request over limits`() {
        val request = SearchPackagesRequest(
            query = "y".repeat(MAX_QUERY_LENGTH + 1),
            targetGroupFilters = List(MAX_TARGET_GROUP_FILTERS + 1) { mapOf(TargetGroup.JVM to emptySet()) },
            owner = "o".repeat(MAX_OWNER_LENGTH + 1),
        )
        assertEquals(setOf("query", "targetGroupFilters", "owner"), violatedProperties(request))
    }

    private fun violatedProperties(request: Any): Set<String> =
        validator.validate(request).map { it.propertyPath.toString() }.toSet()
}
