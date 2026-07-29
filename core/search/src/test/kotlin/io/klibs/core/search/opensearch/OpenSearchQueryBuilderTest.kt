package io.klibs.core.search.opensearch

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch._types.query_dsl.Query
import java.io.StringWriter

class OpenSearchQueryBuilderTest {

    private fun json(query: Query): String {
        val mapper = JacksonJsonpMapper()
        val writer = StringWriter()
        mapper.jsonProvider().createGenerator(writer).use { query.serialize(it, mapper) }
        return writer.toString()
    }

    @Test
    fun `match carries field and boost`() {
        val out = json(OpenSearchQueryBuilder.match("name", "ktor", 4))
        assertTrue(out.contains("\"name\""))
        assertTrue(out.contains("ktor"))
        assertTrue(out.contains("4.0"))
    }

    @Test
    fun `fuzzy sets fuzziness`() {
        assertTrue(json(OpenSearchQueryBuilder.fuzzy("artifact_ids", "ktr", 2)).contains("\"fuzziness\":\"2\""))
    }

    @Test
    fun `bool with shoulds sets minimum_should_match and filter`() {
        val out = json(
            OpenSearchQueryBuilder.bool(
                shoulds = listOf(OpenSearchQueryBuilder.match("name", "ktor", 4)),
                filters = listOf(OpenSearchQueryBuilder.term("platforms", "JVM")),
            )
        )
        assertTrue(out.contains("\"minimum_should_match\":\"1\""))
        assertTrue(out.contains("\"filter\""))
        assertTrue(out.contains("\"should\""))
    }

    @Test
    fun `bool without shoulds omits minimum_should_match`() {
        val out = json(
            OpenSearchQueryBuilder.bool(
                shoulds = emptyList(),
                filters = listOf(OpenSearchQueryBuilder.term("platforms", "JVM")),
            )
        )
        assertFalse(out.contains("minimum_should_match"))
        assertTrue(out.contains("\"filter\""))
    }

    @Test
    fun `scored wraps in function_score script with multiply`() {
        val out = json(OpenSearchQueryBuilder.scored(OpenSearchQueryBuilder.match("name", "ktor", 4)))
        assertTrue(out.contains("\"function_score\""))
        assertTrue(out.contains("\"script_score\""))
        assertTrue(out.contains("\"boost_mode\":\"multiply\""))
    }

    @Test
    fun `common filters map platforms owner and js target group`() {
        val out = json(
            OpenSearchQueryBuilder.bool(
                shoulds = emptyList(),
                filters = OpenSearchQueryBuilder.commonFilters(
                    platforms = listOf(PackagePlatform.JVM),
                    targetFilters = mapOf(TargetGroup.JavaScript to emptySet()),
                    ownerLogin = "jetbrains",
                ),
            )
        )
        assertTrue(out.contains("\"platforms\""))
        assertTrue(out.contains("\"owner_login.keyword\""))
        assertTrue(out.contains("jetbrains"))
        // JavaScript group folds to a platforms=JS term
        assertTrue(out.contains("JS"))
    }

    @Test
    fun `iOS target group expands to native target terms`() {
        val out = json(
            OpenSearchQueryBuilder.bool(
                shoulds = emptyList(),
                filters = OpenSearchQueryBuilder.commonFilters(
                    platforms = emptyList(),
                    targetFilters = mapOf(TargetGroup.IOS to emptySet()),
                    ownerLogin = null,
                ),
            )
        )
        assertTrue(out.contains("\"targets\""))
        assertTrue(out.contains("NATIVE_ios_arm64"))
    }

    @Test
    fun `test target filters with empty target filters`() {
        assertEquals("[]", targetJson(emptyMap()))
    }

    @Test
    fun `test target filters with JavaScript target group only`() {
        assertEquals(
            """[{"term":{"platforms":{"value":"JS"}}}]""",
            targetJson(mapOf(TargetGroup.JavaScript to setOf("js_ir", "js_legacy"))),
        )
    }

    @Test
    fun `test target filters with target group having empty set`() {
        assertEquals(
            """
            [{"terms":{"targets":["JVM_1.6","JVM_1.7","JVM_1.8","JVM_9","JVM_10","JVM_11","JVM_12",
            "JVM_13","JVM_14","JVM_15","JVM_16","JVM_17","JVM_18","JVM_19","JVM_20","JVM_21",
            "JVM_22","JVM_23","JVM_24"]}}]
            """.flat(),
            targetJson(mapOf(TargetGroup.JVM to emptySet())),
        )
    }

    @Test
    fun `test target filters with target group having specific targets`() {
        assertEquals(
            """
            [{"terms":{"targets":["JVM_11","JVM_12","JVM_13","JVM_14","JVM_15","JVM_16","JVM_17",
            "JVM_18","JVM_19","JVM_20","JVM_21","JVM_22","JVM_23","JVM_24"]}}]
            """.flat(),
            targetJson(mapOf(TargetGroup.JVM to setOf("11", "17"))),
        )
    }

    @Test
    fun `test target filters with multiple target groups`() {
        assertEquals(
            """
            [{"terms":{"targets":["JVM_11","JVM_12","JVM_13","JVM_14","JVM_15","JVM_16","JVM_17",
            "JVM_18","JVM_19","JVM_20","JVM_21","JVM_22","JVM_23","JVM_24"]}},
            {"term":{"targets":{"value":"NATIVE_macos_arm64"}}}]
            """.flat(),
            targetJson(
                mapOf(TargetGroup.JVM to setOf("11", "17"), TargetGroup.MacOS to setOf("macos_arm64")),
            ),
        )
    }

    @Test
    fun `test target filters with mixed empty and non-empty target sets`() {
        assertEquals(
            """
            [{"terms":{"targets":["JVM_1.6","JVM_1.7","JVM_1.8","JVM_9","JVM_10","JVM_11","JVM_12",
            "JVM_13","JVM_14","JVM_15","JVM_16","JVM_17","JVM_18","JVM_19","JVM_20","JVM_21",
            "JVM_22","JVM_23","JVM_24"]}},{"term":{"targets":{"value":"NATIVE_macos_arm64"}}},
            {"term":{"targets":{"value":"NATIVE_macos_x64"}}}]
            """.flat(),
            targetJson(
                mapOf(
                    TargetGroup.JVM to emptySet(),
                    TargetGroup.MacOS to setOf("macos_arm64", "macos_x64"),
                ),
            ),
        )
    }

    @Test
    fun `test target filters with JavaScript and other target groups`() {
        assertEquals(
            """
            [{"term":{"platforms":{"value":"JS"}}},{"terms":{"targets":["JVM_11","JVM_12","JVM_13",
            "JVM_14","JVM_15","JVM_16","JVM_17","JVM_18","JVM_19","JVM_20","JVM_21","JVM_22",
            "JVM_23","JVM_24"]}}]
            """.flat(),
            targetJson(
                mapOf(TargetGroup.JavaScript to setOf("js_ir"), TargetGroup.JVM to setOf("11")),
            ),
        )
    }

    @Test
    fun `test target filters with AndroidJVM with empty set`() {
        assertEquals(
            """
            [{"terms":{"targets":["ANDROIDJVM_1.6","ANDROIDJVM_1.7","ANDROIDJVM_1.8","ANDROIDJVM_9",
            "ANDROIDJVM_10","ANDROIDJVM_11","ANDROIDJVM_12","ANDROIDJVM_13","ANDROIDJVM_14",
            "ANDROIDJVM_15","ANDROIDJVM_16","ANDROIDJVM_17","ANDROIDJVM_18","ANDROIDJVM_19",
            "ANDROIDJVM_20","ANDROIDJVM_21","ANDROIDJVM_22","ANDROIDJVM_23","ANDROIDJVM_24"]}}]
            """.flat(),
            targetJson(mapOf(TargetGroup.AndroidJvm to emptySet())),
        )
    }

    @Test
    fun `test target filters with AndroidJVM with specific targets`() {
        assertEquals(
            """
            [{"terms":{"targets":["ANDROIDJVM_11","ANDROIDJVM_12","ANDROIDJVM_13","ANDROIDJVM_14",
            "ANDROIDJVM_15","ANDROIDJVM_16","ANDROIDJVM_17","ANDROIDJVM_18","ANDROIDJVM_19",
            "ANDROIDJVM_20","ANDROIDJVM_21","ANDROIDJVM_22","ANDROIDJVM_23","ANDROIDJVM_24"]}}]
            """.flat(),
            targetJson(mapOf(TargetGroup.AndroidJvm to setOf("11", "17"))),
        )
    }

    @Test
    fun `test target filters with both JVM and AndroidJVM target groups`() {
        assertEquals(
            """
            [{"terms":{"targets":["JVM_17","JVM_18","JVM_19","JVM_20","JVM_21","JVM_22","JVM_23",
            "JVM_24"]}},{"terms":{"targets":["ANDROIDJVM_15","ANDROIDJVM_16","ANDROIDJVM_17",
            "ANDROIDJVM_18","ANDROIDJVM_19","ANDROIDJVM_20","ANDROIDJVM_21","ANDROIDJVM_22",
            "ANDROIDJVM_23","ANDROIDJVM_24"]}}]
            """.flat(),
            targetJson(
                mapOf(TargetGroup.JVM to setOf("17"), TargetGroup.AndroidJvm to setOf("15")),
            ),
        )
    }

    private fun targetJson(targetFilters: Map<TargetGroup, Set<String>>): String =
        OpenSearchQueryBuilder.commonFilters(emptyList(), targetFilters, null)
            .joinToString(",", "[", "]") { json(it) }

    private fun String.flat() = trimIndent().replace("\n", "")

}
