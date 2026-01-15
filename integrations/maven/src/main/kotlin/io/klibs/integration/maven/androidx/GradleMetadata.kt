package io.klibs.integration.maven.androidx

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Based on Gradle module metadata specification 1.0
 * https://github.com/gradle/gradle/blob/f6a98158e75a636245f70d46604fcab3152361e8/subprojects/docs/src/docs/design/gradle-module-metadata-1.0-specification.md
 */
data class GradleMetadata(
    val component: Component? = null,
    val variants: List<Variant>? = null,
    val createdBy: CreatedBy? = null,
) {
    fun isRootKMP(): Boolean {
        if (component?.url != null) return false  // that is not a root package

        return variants?.any { variant ->
            variant.attributes?.get("org.jetbrains.kotlin.platform.type")?.let {
                it == "common"
            } == true
        } == true
    }
}

data class CreatedBy(
    val gradle: Gradle? = null,
)

data class Gradle(
    val version: String,
)

data class Component(
    val url: String? = null,
)

data class Variant(
    val attributes: Attributes? = null,
)


typealias Attributes = Map<String, String>

data class ModuleMetadataWrapper(
    val gradleMetadata: GradleMetadata,
    val releasedAt: Instant
)