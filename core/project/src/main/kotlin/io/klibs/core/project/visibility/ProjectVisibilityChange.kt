package io.klibs.core.project.visibility

/**
 * Outcome of a manual visibility change.
 */
enum class ProjectVisibilityChange {
    CHANGED,
    ALREADY_IN_THAT_STATE,
    PROJECT_NOT_FOUND
}
