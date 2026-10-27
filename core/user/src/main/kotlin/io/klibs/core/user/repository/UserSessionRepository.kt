package io.klibs.core.user.repository

import io.klibs.core.user.entity.UserSessionEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface UserSessionRepository : CrudRepository<UserSessionEntity, UUID> {
    fun findByTokenHash(tokenHash: String): UserSessionEntity?

    @Modifying
    @Query(
        """
            UPDATE UserSessionEntity
            SET expiresAt = :newExpiresAt
            WHERE id = :id
        """
    )
    fun updateExpiration(
        @Param("id") id: UUID,
        @Param("newExpiresAt") newExpiresAt: Instant,
    ): Long

    @Modifying
    @Query("DELETE FROM UserSessionEntity WHERE tokenHash = :tokenHash")
    fun deleteByTokenHash(
        @Param("tokenHash") tokenHash: String,
    ): Int
}
