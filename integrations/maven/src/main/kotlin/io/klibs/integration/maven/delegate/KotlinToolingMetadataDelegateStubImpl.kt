package io.klibs.integration.maven.delegate

import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.androidx.Variant
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata

class KotlinToolingMetadataDelegateStubImpl(metadata: GradleMetadata) : KotlinToolingMetadataDelegate {

    override val schemaVersion: String = "1.1.0"
    override val buildSystem: String = metadata.createdBy?.gradle?.let { "Gradle" } ?: "Unknown"
    override val buildSystemVersion: String = metadata.createdBy?.gradle?.version ?: "Unknown"
    override val kotlinVersion: String = "Unknown"
    override val projectTargets: List<KotlinToolingMetadata.ProjectTargetMetadata> =
        metadata.variants?.mapNotNull { variant -> createProjectTargetMetadata(variant) } ?: emptyList()

    private fun createProjectTargetMetadata(variant: Variant): KotlinToolingMetadata.ProjectTargetMetadata? {
        val platformType = variant.attributes?.get("org.jetbrains.kotlin.platform.type") ?: return null
        val platformTarget = variant.attributes.get("org.jetbrains.kotlin.native.target") ?: ""
        return KotlinToolingMetadata.ProjectTargetMetadata(
            platformTarget,
            platformType,
            fillExtras(platformType, platformTarget)
        )
    }

    private fun fillExtras(platformType: String, platformTarget: String): KotlinToolingMetadata.ProjectTargetMetadata.Extras {
        return when (platformType) {
            "jvm" -> KotlinToolingMetadata.ProjectTargetMetadata.Extras(
                KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras(
                    platformTarget,
                    true
                )
            )
            "androidJvm" -> KotlinToolingMetadata.ProjectTargetMetadata.Extras(
                android = KotlinToolingMetadata.ProjectTargetMetadata.AndroidExtras(
                    platformTarget,
                    platformTarget
                )
            )
            "native" -> KotlinToolingMetadata.ProjectTargetMetadata.Extras(
                native = KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras(
                    platformTarget,
                    "",
                    ""
                )
            )
            else -> KotlinToolingMetadata.ProjectTargetMetadata.Extras()
        }
    }
}