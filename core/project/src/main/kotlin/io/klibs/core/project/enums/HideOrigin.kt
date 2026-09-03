package io.klibs.core.project.enums

/**
 * What put a project into `project_hidden`.
 *
 * Only [AUTO] rows are removed automatically, when the SCM repository becomes reachable again.
 */
enum class HideOrigin {
    AUTO,
    MANUAL
}
