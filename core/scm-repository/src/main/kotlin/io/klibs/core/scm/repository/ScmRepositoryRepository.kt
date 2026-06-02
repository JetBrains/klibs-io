package io.klibs.core.scm.repository

import java.time.Instant

interface ScmRepositoryRepository {

    fun upsert(entity: ScmRepositoryEntity): ScmRepositoryEntity

    fun update(entity: ScmRepositoryEntity): ScmRepositoryEntity

    fun setUpdatedAt(id: Int, updatedAt: Instant): Boolean

    fun findById(id: Int): ScmRepositoryEntity?

    fun findByNativeId(nativeId: Long): ScmRepositoryEntity?

    fun findByName(ownerLogin: String, name: String): ScmRepositoryEntity?

    fun findIdByName(ownerLogin: String, name: String): Int?

    /**
     * Next [limit] repos due for a GitHub refresh, highest-star first.
     * Skips repos still within their backoff window (see `scm_repo_scheduling.next_retry_at`).
     */
    fun findMultipleForUpdate(limit: Int = 3): List<ScmRepositoryEntity>
}

