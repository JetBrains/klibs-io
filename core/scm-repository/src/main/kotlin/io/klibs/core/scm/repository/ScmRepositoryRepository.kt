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

    fun findMultipleForUpdate(limit: Int = 3): List<ScmRepositoryEntity>

    /**
     * Picks repos whose sliding-window issue/PR event snapshot is oldest (or never synced).
     * Intended for the OSS health event-sync queue. Queue state lives on
     * `scm_repo_health_components`; this method `LEFT JOIN`s for ordering.
     */
    fun findMultipleForHealthEventSync(limit: Int = 1): List<ScmRepositoryEntity>

    /**
     * Picks repos whose health score is due to be recomputed. Only considers repos that already
     * have a row in `scm_repo_health_components` — i.e. event sync has run for them at least once.
     */
    fun findMultipleForHealthScoreCompute(limit: Int = 1): List<ScmRepositoryEntity>
}

