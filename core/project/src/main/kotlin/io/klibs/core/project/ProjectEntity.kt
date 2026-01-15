package io.klibs.core.project

import java.time.Instant

data class ProjectEntity(
    val id: Int?,
    val scmRepoId: Int,

    val description: String?,

    val latestVersion: String,
    val latestVersionTs: Instant,
) {
    val idNotNull: Int get() = requireNotNull(id)
}
