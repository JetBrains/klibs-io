package io.klibs.notifier

import java.io.Serializable

internal data class ArtifactCoordinates(
    val groupId: String,
    val artifactId: String,
    val version: String,
) : Serializable
