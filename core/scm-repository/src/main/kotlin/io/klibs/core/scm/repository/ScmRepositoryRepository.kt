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
}

