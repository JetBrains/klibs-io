package io.klibs.core.search.repository

import io.klibs.core.pckg.model.TargetGroup

/**
 * Builds a boolean SQL predicate for the target filters.
 *
 * Groups within the same map are combined with `OR`, separate maps in the list are combined with `AND`.
 * JS/Wasm are matched on `platforms_vector`, all other groups on `targets_vector`.
 */
internal fun formTargetCondition(targetFilters: List<Map<TargetGroup, Set<String>>>): String? {
    if (targetFilters.isEmpty()) return null

    val andGroups = targetFilters.mapNotNull { orGroup ->
        val orConditions = orGroup.mapNotNull { (group, targets) -> groupSqlPredicate(group, targets) }
        when {
            orConditions.isEmpty() -> null
            orConditions.size == 1 -> orConditions.single()
            else -> "(${orConditions.joinToString(" OR ")})"
        }
    }

    return andGroups.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
}

private fun groupSqlPredicate(group: TargetGroup, targets: Set<String>): String? = when {
    group == TargetGroup.JavaScript -> "platforms_vector @@ 'JS'"
    group == TargetGroup.Wasm -> "platforms_vector @@ 'WASM'"
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
        "targets_vector @@ '($compatibleTargets)'::tsquery"
    }

    group == TargetGroup.Unknown -> null

    // If a target group has an empty set, it means "any target in this group".
    targets.isEmpty() -> group.targets
        .joinToString(" | ") { "${group.platformName}_$it" }
        .let { "targets_vector @@ '($it)'::tsquery" }

    else -> {
        val specificTargets = targets.joinToString(" & ") { "${group.platformName}_$it" }
        "targets_vector @@ '($specificTargets)'::tsquery"
    }
}