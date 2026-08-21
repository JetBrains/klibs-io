package io.klibs.core.pckg.model

import kotlin.collections.emptySet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TargetGroupsTest {

    @Test
    fun `groupTargets groups multiple targets by group`() {
        val targets = listOf(
            PackageTarget(PackagePlatform.NATIVE, "ios_arm64"),
            PackageTarget(PackagePlatform.NATIVE, "ios_x64"),
            PackageTarget(PackagePlatform.JVM, "17"),
        )

        val grouped = TargetGroup.gatherTargetGroupsFromTargets(targets)

        assertEquals(2, grouped.size)
        assertEquals(setOf("ios_arm64", "ios_x64"), grouped[TargetGroup.IOS])
        assertEquals(setOf("17"), grouped[TargetGroup.JVM])
    }

    @Test
    fun `groupTargets processes null and blank targets`() {
        val targets = listOf(
            PackageTarget(PackagePlatform.JS, null),
            PackageTarget(PackagePlatform.COMMON, ""),
            PackageTarget(PackagePlatform.NATIVE, "ios_arm64"),
        )

        val targetGroups = TargetGroup.gatherTargetGroupsFromTargets(targets)

        assertEquals(2, targetGroups.size)
        assertEquals(mapOf(TargetGroup.IOS to setOf("ios_arm64"), TargetGroup.JavaScript to emptySet()), targetGroups)
    }


    @Test
    fun `splits underscore-containing targets`() {
        val tokens = listOf("NATIVE_ios_arm64", "NATIVE_ios_x64", "JVM_17")

        val grouped = TargetGroup.getTargetGroupsFromTargets(tokens)

        assertEquals(2, grouped.size)
        assertEquals(setOf("ios_arm64", "ios_x64"), grouped[TargetGroup.IOS])
        assertEquals(setOf("17"), grouped[TargetGroup.JVM])
    }

    @Test
    fun `skips bare tokens without concrete target`() {
        val tokens = listOf("JS", "COMMON", "NATIVE_ios_arm64")

        val grouped = TargetGroup.getTargetGroupsFromTargets(tokens)

        assertEquals(2, grouped.size)
        assertEquals(mapOf(TargetGroup.IOS to setOf("ios_arm64"), TargetGroup.JavaScript to emptySet()), grouped)
    }

    @Test
    fun `groupTargetTokens puts unknown token into Unknown group`() {
        val tokens = listOf("NATIVE_some_unknown_target")

        val grouped = TargetGroup.getTargetGroupsFromTargets(tokens)

        assertEquals(setOf("some_unknown_target"), grouped[TargetGroup.Unknown])
    }

    @Test
    fun `groupTargetTokens returns empty map for empty input`() {
        assertTrue(TargetGroup.getTargetGroupsFromTargets(emptyList()).isEmpty())
    }
}
