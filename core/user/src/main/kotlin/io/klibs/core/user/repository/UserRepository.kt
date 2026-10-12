package io.klibs.core.user.repository

import io.klibs.core.user.entity.UserEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : CrudRepository<UserEntity, UUID> {
    fun findByExternalUserIdHash(externalUserIdHash: String): UserEntity?

    @Modifying
    @Query(
        """
            INSERT INTO UserEntity (id, externalUserIdHash)
            VALUES (:id, :externalUserIdHash)
            ON CONFLICT(externalUserIdHash) DO NOTHING
        """
    )
    fun saveIfAbsent(
        @Param("id") id: UUID,
        @Param("externalUserIdHash") externalUserIdHash: String,
    ): Long
}
