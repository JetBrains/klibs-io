package io.klibs.core.search

import io.klibs.core.pckg.model.TargetGroup

internal fun formTargetCondition(targetFilters: Map<TargetGroup, Set<String>>): String? {
    if (targetFilters.isEmpty()) return null

    val conditions = targetFilters.mapNotNull { (group, targets) ->
        when {
            group == TargetGroup.JavaScript -> null
            group == TargetGroup.Wasm -> null
            group in setOf(TargetGroup.JVM, TargetGroup.AndroidJvm) -> {
                // Compare targets by their order in the target list
                val targetIndices = targets.map { t ->
                    val idx = group.targets.indexOf(t)
                    if (idx >= 0) idx else error("target not found: $t; group: $group")
                }
                val startIndex = targetIndices.minOrNull() ?: 0
                val compatibleTargets = group.targets
                    .drop(startIndex)
                    .joinToString(" | ") { "${group.platformName}_$it" }
                "($compatibleTargets)"
            }

            // If a target group has an empty set, it means "any target in this group"
            targets.isEmpty() -> {
                val allTargets = group.targets
                    .joinToString(" | ") { "${group.platformName}_$it" }
                "($allTargets)"
            }

            else -> targets.joinToString(" & ") { "${group.platformName}_$it" }
        }
    }

    return conditions.takeIf { it.isNotEmpty() }
        ?.let { "'(${it.joinToString(" & ")})'::tsquery" }
}