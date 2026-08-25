package io.klibs.app.util

import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.integration.maven.MavenArtifact

fun MavenArtifact.toIndexRequest(
    reindex: Boolean = false,
    userRequestIssue: UserRequestIssueEntity? = null,
): IndexingRequestEntity {
    return IndexingRequestEntity(
        groupId = this.groupId,
        artifactId = this.artifactId,
        version = this.version,
        repo = this.scraperType,
        reindex = reindex,
        userRequestIssue = userRequestIssue,
    )
}
