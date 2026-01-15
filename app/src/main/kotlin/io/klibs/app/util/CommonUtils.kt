package io.klibs.app.util

import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.integration.maven.MavenArtifact

fun MavenArtifact.toIndexRequest(reindex: Boolean = false): IndexingRequestEntity {
    return IndexingRequestEntity(
        groupId = this.groupId,
        artifactId = this.artifactId,
        version = this.version,
        releasedAt = this.releasedAt,
        repo = this.scraperType,
        reindex = reindex
    )
}
