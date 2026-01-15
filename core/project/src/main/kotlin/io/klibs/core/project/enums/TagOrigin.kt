package io.klibs.core.project.enums

/**
 * Origin of a project tag entry.
 *
 * Stored as string in the DB (see PackageEntity @Enumerated reference).
 */
enum class TagOrigin {
    USER,
    GITHUB,
    AI
}
