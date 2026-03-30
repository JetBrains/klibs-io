package io.klibs.core.readme.entity

import java.time.Instant

data class ReadmeMetadataEntity(
    val id: Int? = null,
    val scmRepoId: Int,
    val lastSyncedAt: Instant,
    val lastProcessedAt: Instant
)