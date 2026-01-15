package io.klibs.core.owner

interface ScmOwnerRepository {

    fun upsert(entity: ScmOwnerEntity): ScmOwnerEntity

    fun updateLoginByNativeId(nativeId: Long, newLogin: String): Boolean

    fun findById(id: Int): ScmOwnerEntity?

    fun findByLogin(login: String): ScmOwnerEntity?

    fun findForUpdate(): ScmOwnerEntity?
}

