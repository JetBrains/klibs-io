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
        val orConditions =
            orGroup.mapNotNull { (group, targets) -> createOrPredicateWithinOneTargetGroup(group, targets) }
        when {
            orConditions.isEmpty() -> null
            orConditions.size == 1 -> orConditions.single()
            else -> "(${orConditions.joinToString(" OR ")})"
        }
    }

    return andGroups.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
}

private fun createOrPredicateWithinOneTargetGroup(targetGroup: TargetGroup, targets: Set<String>): String? = when {
    targetGroup == TargetGroup.JavaScript -> "platforms_vector @@ 'JS'"
    targetGroup == TargetGroup.Wasm -> "platforms_vector @@ 'WASM'"
    targetGroup in setOf(TargetGroup.JVM, TargetGroup.AndroidJvm) -> {
        // Compare targets by their order in the target list
        val targetIndices = targets.map { t ->
            val idx = targetGroup.targets.indexOf(t)
            if (idx >= 0) idx else error("target not found: $t; group: $targetGroup")
        }
        val startIndex = targetIndices.minOrNull() ?: 0
        val compatibleTargets = targetGroup.targets
            .drop(startIndex)
            .joinToString(" | ") { "${targetGroup.platform}_$it" }
        "targets_vector @@ '($compatibleTargets)'::tsquery"
    }

    targetGroup == TargetGroup.Unknown -> null

    // If a target group has an empty set, it means "any target in this group".
    targets.isEmpty() -> targetGroup.targets
        .joinToString(" | ") { "${targetGroup.platform}_$it" }
        .let { "targets_vector @@ '($it)'::tsquery" }

    else -> {
        val specificTargets = targets.joinToString(" & ") { "${targetGroup.platform}_$it" }
        "targets_vector @@ '($specificTargets)'::tsquery"
    }
}