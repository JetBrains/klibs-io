package io.klibs.core.search.dto.validation

import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class TargetGroupFilterValidationTest {

    @Test
    fun `accepts empty and null filters`() {
        assertNull(validateTargetGroupFilters(null))
        assertNull(validateTargetGroupFilters(emptyList()))
    }

    @ParameterizedTest
    @EnumSource(TargetGroup::class)
    fun `accepts valid group with valid targets`(targetGroup: TargetGroup) {
        if (targetGroup == TargetGroup.Unknown) return // unknown target group should fail validation
        assertNull(validateTargetGroupFilters(listOf(mapOf(targetGroup to targetGroup.targets.toSet()))))
    }

    @Test
    fun `accepts valid group with empty target set`() {
        assertNull(validateTargetGroupFilters(listOf(mapOf(TargetGroup.IOS to emptySet()))))
    }

    @Test
    fun `rejects Unknown target group with empty set`() {
        assertNotNull(validateTargetGroupFilters(listOf(mapOf(TargetGroup.Unknown to emptySet()))))
    }

    @Test
    fun `rejects Unknown target group even when combined with a valid group`() {
        val filters = listOf(mapOf(TargetGroup.JVM to setOf("11"), TargetGroup.Unknown to emptySet()))
        assertNotNull(validateTargetGroupFilters(filters))
    }

    @Test
    fun `rejects targets that do not belong to the group`() {
        assertNotNull(validateTargetGroupFilters(listOf(mapOf(TargetGroup.JVM to setOf("not-a-jvm-target")))))
    }
}
