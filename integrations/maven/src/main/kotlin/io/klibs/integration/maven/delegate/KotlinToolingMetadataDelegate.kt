package io.klibs.integration.maven.delegate

import org.jetbrains.kotlin.tooling.KotlinToolingMetadata.ProjectTargetMetadata

sealed interface KotlinToolingMetadataDelegate  {

    val schemaVersion: String
    val buildSystem: String
    val buildSystemVersion: String
    val kotlinVersion: String
    val projectTargets: List<ProjectTargetMetadata>
}