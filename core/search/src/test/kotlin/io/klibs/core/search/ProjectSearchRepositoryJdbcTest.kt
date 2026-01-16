package io.klibs.core.search

import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.jdbc.core.simple.JdbcClient
import io.mockk.mockk

class ProjectSearchRepositoryJdbcTest {
    private val jdbcClient: JdbcClient = mockk()
    private val projectSearchRepository = ProjectSearchRepositoryJdbc(jdbcClient)

    @Test
    fun `test wildCardsQuery with simple word`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("kotlin")
        assertEquals("", withSpecial)
        assertEquals("kotlin:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with multiple words`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("kotlin java")
        assertEquals("", withSpecial)
        assertEquals("kotlin:*|java:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with special characters`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("api-server:1.0")
        assertEquals("'api-server:1.0':*", withSpecial)
        assertEquals("api:*|server:*|1:*|0:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with apostrophes`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("user's guide")
        assertEquals("'user''s':*", withSpecial)
        assertEquals("guide:*|user:*|s:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with multiple spaces`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("  kotlin    java  ")
        assertEquals("", withSpecial)
        assertEquals("kotlin:*|java:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with comma separated words`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("kotlin,java")
        assertEquals("'kotlin,java':*", withSpecial)
        assertEquals("kotlin:*|java:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with mixed special characters and spaces`() {
        val (withSpecial, withoutSpecial) = projectSearchRepository.createWildcardSubqueries("spring-boot test:unit kotlin")
        assertEquals("'spring-boot':*|'test:unit':*", withSpecial)
        assertEquals("kotlin:*|spring:*|boot:*|test:*|unit:*", withoutSpecial)
    }

    @Test
    fun `test wildCardsQuery with blank input throws assertion error`() {
        assertThrows(AssertionError::class.java) {
            projectSearchRepository.createWildcardSubqueries("   ")
        }
        assertThrows(AssertionError::class.java) {
            projectSearchRepository.createWildcardSubqueries("")
        }
    }

    @Test
    fun `test formTargetCondition with empty target filters`() {
        val result = formTargetCondition(emptyMap())
        assertNull(result, "Empty target filters should return null")
    }

    @Test
    fun `test formTargetCondition with JavaScript target group only`() {
        val targetFilters = mapOf(
            TargetGroup.JavaScript to setOf("js_ir", "js_legacy")
        )
        val result = formTargetCondition(targetFilters)
        assertNull(result, "JavaScript target group should be skipped and result in null")
    }

    @Test
    fun `test formTargetCondition with target group having empty set`() {
        val targetFilters = mapOf(
            TargetGroup.JVM to emptySet<String>()
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((JVM_1.6 | JVM_1.7 | JVM_1.8 | JVM_9 | JVM_10 | JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24))'::tsquery", result)
    }

    @Test
    fun `test formTargetCondition with target group having specific targets`() {
        val targetFilters = mapOf(
            TargetGroup.JVM to setOf("11", "17")
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24))'::tsquery", result)
    }

    @Test
    fun `test formTargetCondition with multiple target groups`() {
        val targetFilters = mapOf(
            TargetGroup.JVM to setOf("11", "17"),
            TargetGroup.MacOS to setOf("macos_arm64")
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24) & NATIVE_macos_arm64)'::tsquery", result)
    }

    @Test
    fun `test formTargetCondition with mixed empty and non-empty target sets`() {
        val targetFilters = mapOf(
            TargetGroup.JVM to emptySet(),
            TargetGroup.MacOS to setOf("macos_arm64", "macos_x64")
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((JVM_1.6 | JVM_1.7 | JVM_1.8 | JVM_9 | JVM_10 | JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24) & NATIVE_macos_arm64 & NATIVE_macos_x64)'::tsquery", result)
    }

    @Test
    fun `test formTargetCondition with JavaScript and other target groups`() {
        val targetFilters = mapOf(
            TargetGroup.JavaScript to setOf("js_ir"),
            TargetGroup.JVM to setOf("11")
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((JVM_11 | JVM_12 | JVM_13 | JVM_14 | JVM_15 | JVM_16 | JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24))'::tsquery", result, "JavaScript target group should be skipped")
    }

    @Test
    fun `test formTargetCondition with AndroidJVM with empty set`() {
        val targetFilters = mapOf(
            TargetGroup.AndroidJvm to emptySet<String>()
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((ANDROIDJVM_1.6 | ANDROIDJVM_1.7 | ANDROIDJVM_1.8 | ANDROIDJVM_9 | ANDROIDJVM_10 | ANDROIDJVM_11 | ANDROIDJVM_12 | ANDROIDJVM_13 | ANDROIDJVM_14 | ANDROIDJVM_15 | ANDROIDJVM_16 | ANDROIDJVM_17 | ANDROIDJVM_18 | ANDROIDJVM_19 | ANDROIDJVM_20 | ANDROIDJVM_21 | ANDROIDJVM_22 | ANDROIDJVM_23 | ANDROIDJVM_24))'::tsquery", result)
    }

    @Test
    fun `test formTargetCondition with AndroidJVM with specific targets`() {
        val targetFilters = mapOf(
            TargetGroup.AndroidJvm to setOf("11", "17")
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((ANDROIDJVM_11 | ANDROIDJVM_12 | ANDROIDJVM_13 | ANDROIDJVM_14 | ANDROIDJVM_15 | ANDROIDJVM_16 | ANDROIDJVM_17 | ANDROIDJVM_18 | ANDROIDJVM_19 | ANDROIDJVM_20 | ANDROIDJVM_21 | ANDROIDJVM_22 | ANDROIDJVM_23 | ANDROIDJVM_24))'::tsquery", result)
    }

    @Test
    fun `test formTargetCondition with both JVM and AndroidJVM target groups`() {
        val targetFilters = mapOf(
            TargetGroup.JVM to setOf("17"),
            TargetGroup.AndroidJvm to setOf("15")
        )
        val result = formTargetCondition(targetFilters)
        assertEquals("'((JVM_17 | JVM_18 | JVM_19 | JVM_20 | JVM_21 | JVM_22 | JVM_23 | JVM_24) & (ANDROIDJVM_15 | ANDROIDJVM_16 | ANDROIDJVM_17 | ANDROIDJVM_18 | ANDROIDJVM_19 | ANDROIDJVM_20 | ANDROIDJVM_21 | ANDROIDJVM_22 | ANDROIDJVM_23 | ANDROIDJVM_24))'::tsquery", result)
    }
}
