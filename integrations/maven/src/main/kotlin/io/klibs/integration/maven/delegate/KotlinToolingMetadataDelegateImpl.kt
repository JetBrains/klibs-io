package io.klibs.integration.maven.delegate

import org.jetbrains.kotlin.tooling.KotlinToolingMetadata

class KotlinToolingMetadataDelegateImpl(val kotlinToolingMetadata: KotlinToolingMetadata) :
    KotlinToolingMetadataDelegate {
    override val schemaVersion: String
        get() = kotlinToolingMetadata.schemaVersion
    override val buildSystem: String
        get() = kotlinToolingMetadata.buildSystem
    override val buildSystemVersion: String
        get() = kotlinToolingMetadata.buildSystemVersion
    override val kotlinVersion: String
        get() = kotlinToolingMetadata.buildPluginVersion
    override val projectTargets: List<KotlinToolingMetadata.ProjectTargetMetadata>
        get() = kotlinToolingMetadata.projectTargets
}