package io.klibs.core.search.repository

import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.jdbc.core.simple.JdbcClient

class ProjectSearchRepositoryJdbcTest {
    private val jdbcClient: JdbcClient = mock()
    private val projectSearchRepository = ProjectSearchRepositoryJdbc(jdbcClient)

    @Test
    fun `test wildCardsQuery with simple word`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("kotlin")
        Assertions.assertEquals("", withSpecial)
        Assertions.assertEquals("kotlin:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with multiple words`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("kotlin java")
        Assertions.assertEquals("", withSpecial)
        Assertions.assertEquals("kotlin:*|java:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with special characters`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("api-server:1.0")
        Assertions.assertEquals("'api-server:1.0':*", withSpecial)
        Assertions.assertEquals("api:*|server:*|1:*|0:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with apostrophes`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("user's guide")
        Assertions.assertEquals("'user''s':*", withSpecial)
        Assertions.assertEquals("guide:*|user:*|s:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with multiple spaces`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("  kotlin    java  ")
        Assertions.assertEquals("", withSpecial)
        Assertions.assertEquals("kotlin:*|java:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with comma separated words`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("kotlin,java")
        Assertions.assertEquals("'kotlin,java':*", withSpecial)
        Assertions.assertEquals("kotlin:*|java:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with mixed special characters and spaces`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("spring-boot test:unit kotlin")
        Assertions.assertEquals("'spring-boot':*|'test:unit':*", withSpecial)
        Assertions.assertEquals("kotlin:*|spring:*|boot:*|test:*|unit:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with blank input throws assertion error`() {
        Assertions.assertThrows(AssertionError::class.java) {
            projectSearchRepository.createWildcardSubqueries("   ")
        }
        Assertions.assertThrows(AssertionError::class.java) {
            projectSearchRepository.createWildcardSubqueries("")
        }
    }

    @Test
    fun `test formTargetCondition with empty target filters`() {
        val result = formTargetCondition(emptyList())
        Assertions.assertNull(result, "Empty target filters should return null")
    }

    @Test
    fun `test formTargetCondition with JavaScript target group only`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JavaScript to setOf("js_ir", "js_legacy"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "platforms_vector @@ 'JS'",
            result,
            "JavaScript target group should match on platforms_vector"
        )
    }

    @Test
    fun `test formTargetCondition with target group having empty set`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JVM to emptySet<String>())
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(JVM_1.6 | JVM_1.7 | JVM_1.8 | JVM_9 | JVM_10 | JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with target group having specific targets`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JVM to setOf("11", "17"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with multiple target groups is combined with AND`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JVM to setOf("11", "17")),
            mapOf(TargetGroup.MacOS to setOf("macos_arm64"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery AND targets_vector @@ '(NATIVE_macos_arm64)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with mixed empty and non-empty target sets`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JVM to emptySet()),
            mapOf(TargetGroup.MacOS to setOf("macos_arm64", "macos_x64"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(JVM_1.6 | JVM_1.7 | JVM_1.8 | JVM_9 | JVM_10 | JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery AND targets_vector @@ '(NATIVE_macos_arm64 & NATIVE_macos_x64)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with JavaScript and other target groups`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JavaScript to setOf("js_ir")),
            mapOf(TargetGroup.JVM to setOf("11"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "platforms_vector @@ 'JS' AND targets_vector @@ '(JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with AndroidJVM with empty set`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.AndroidJvm to emptySet<String>())
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(ANDROIDJVM_1.6 | ANDROIDJVM_1.7 | ANDROIDJVM_1.8 | ANDROIDJVM_9 | ANDROIDJVM_10 | ANDROIDJVM_11 | ANDROIDJVM_12 | ANDROIDJVM_13 | ANDROIDJVM_14 | ANDROIDJVM_15 | ANDROIDJVM_16 | ANDROIDJVM_17 | ANDROIDJVM_18 | ANDROIDJVM_19 | ANDROIDJVM_20 | ANDROIDJVM_21 | ANDROIDJVM_22 | ANDROIDJVM_23 | ANDROIDJVM_24 | ANDROIDJVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with AndroidJVM with specific targets`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.AndroidJvm to setOf("11", "17"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(ANDROIDJVM_11 | ANDROIDJVM_12 | ANDROIDJVM_13 | ANDROIDJVM_14 | ANDROIDJVM_15 | ANDROIDJVM_16 | ANDROIDJVM_17 | ANDROIDJVM_18 | ANDROIDJVM_19 | ANDROIDJVM_20 | ANDROIDJVM_21 | ANDROIDJVM_22 | ANDROIDJVM_23 | ANDROIDJVM_24 | ANDROIDJVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition with both JVM and AndroidJVM target groups`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JVM to setOf("17")),
            mapOf(TargetGroup.AndroidJvm to setOf("15"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery AND targets_vector @@ '(ANDROIDJVM_15 | ANDROIDJVM_16 | ANDROIDJVM_17 | ANDROIDJVM_18 | ANDROIDJVM_19 | ANDROIDJVM_20 | ANDROIDJVM_21 | ANDROIDJVM_22 | ANDROIDJVM_23 | ANDROIDJVM_24 | ANDROIDJVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition combines groups within a map with OR`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JavaScript to emptySet<String>(), TargetGroup.Wasm to emptySet<String>())
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "(platforms_vector @@ 'JS' OR platforms_vector @@ 'WASM')",
            result
        )
    }

    @Test
    fun `test formTargetCondition expands empty native groups to their targets combined with OR within a group and AND between groups`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.MacOS to emptySet<String>()),
            mapOf(TargetGroup.Windows to emptySet<String>())
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(NATIVE_macos_arm64 | NATIVE_macos_x64)'::tsquery AND targets_vector @@ '(NATIVE_mingw_x64 | NATIVE_mingw_x86)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition combines OR groups and AND groups`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.JavaScript to emptySet<String>(), TargetGroup.Wasm to emptySet<String>()),
            mapOf(TargetGroup.JVM to setOf("17"))
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "(platforms_vector @@ 'JS' OR platforms_vector @@ 'WASM') AND targets_vector @@ '(JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24 | JVM_25)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition skips Unknown group without emitting an empty tsquery`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.Unknown to emptySet<String>())
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(null, result)
    }

    @Test
    fun `test formTargetCondition skips Unknown group inside an OR group`() {
        val targetFilters = listOf(
            mapOf(TargetGroup.MacOS to emptySet(), TargetGroup.Unknown to emptySet<String>())
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "targets_vector @@ '(NATIVE_macos_arm64 | NATIVE_macos_x64)'::tsquery",
            result
        )
    }

    @Test
    fun `test formTargetCondition drops Unknown from a large native OR group without trailing OR`() {
        val targetFilters = listOf(
            mapOf(
                TargetGroup.Linux to emptySet(),
                TargetGroup.MacOS to emptySet(),
                TargetGroup.Windows to emptySet(),
                TargetGroup.TvOS to emptySet(),
                TargetGroup.WatchOS to emptySet(),
                TargetGroup.Unknown to emptySet<String>(),
            )
        )
        val result = formTargetCondition(targetFilters)
        Assertions.assertEquals(
            "(targets_vector @@ '(NATIVE_linux_arm32_hfp | NATIVE_linux_arm64 | NATIVE_linux_mips32 | NATIVE_linux_mipsel32 | NATIVE_linux_x64)'::tsquery OR " +
                "targets_vector @@ '(NATIVE_macos_arm64 | NATIVE_macos_x64)'::tsquery OR " +
                "targets_vector @@ '(NATIVE_mingw_x64 | NATIVE_mingw_x86)'::tsquery OR " +
                "targets_vector @@ '(NATIVE_tvos_arm64 | NATIVE_tvos_simulator_arm64 | NATIVE_tvos_x64)'::tsquery OR " +
                "targets_vector @@ '(NATIVE_watchos_arm32 | NATIVE_watchos_arm64 | NATIVE_watchos_device_arm64 | NATIVE_watchos_simulator_arm64 | NATIVE_watchos_x64 | NATIVE_watchos_x86)'::tsquery)",
            result
        )
    }
}