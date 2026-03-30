package io.klibs.core.readme.repository

import io.klibs.core.readme.entity.ReadmeMetadataEntity

interface ReadmeMetadataRepository {
    fun insert(scmRepoId: Int): Boolean
    fun updateLastSyncedAt(scmRepoId: Int): Boolean
    fun updateLastSyncedAndProcessedAt(scmRepoId: Int): Boolean
    fun findByScmRepoId(scmRepoId: Int): ReadmeMetadataEntity?
}