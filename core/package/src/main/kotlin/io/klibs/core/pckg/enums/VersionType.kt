package io.klibs.core.pckg.enums

import io.github.z4kn4fein.semver.toVersionOrNull

enum class VersionType {
    STABLE,
    NON_STABLE,
    NON_SEMVER;

    companion object {
        fun from(version: String): VersionType {
            val semver = version.toVersionOrNull(strict = false) ?: return NON_SEMVER
            return if (semver.isStable) STABLE else NON_STABLE
        }
    }
}
