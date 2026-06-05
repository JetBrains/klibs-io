package io.klibs.app.dto

import io.klibs.integration.maven.dto.MavenArtifactDTO

data class ProcessedUserRequestInfo(
    val request: MavenArtifactDTO,
    val issueNumber: Int
)