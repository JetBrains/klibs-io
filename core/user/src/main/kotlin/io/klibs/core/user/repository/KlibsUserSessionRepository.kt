package io.klibs.core.user.repository

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.entity.UserSessionEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface KlibsUserSessionRepository : CrudRepository<UserSessionEntity, UUID> {
    fun findByTokenHash(tokenHash: String): UserSessionEntity?

    @Modifying
    @Query(
        """
            INSERT INTO UserSessionEntity (id, user, tokenHash, createdAt, expiresAt)
            VALUES (:id, :user, :tokenHash, :createdAt, :expiresAt)
            ON CONFLICT(tokenHash) DO NOTHING
        """
    )
    fun saveIfAbsent(
        @Param("id") id: UUID,
        @Param("user") user: UserEntity,
        @Param("tokenHash") tokenHash: String,
        @Param("createdAt") createdAt: Instant,
        @Param("expiresAt") expiresAt: Instant,
    ): Long

    @Modifying
    @Query(
        """
            UPDATE UserSessionEntity
            SET expiresAt = :newExpiresAt
            WHERE id = :id AND expiresAt = :currentExpiresAt
        """
    )
    fun updateExpiration(
        @Param("id") id: UUID,
        @Param("currentExpiresAt") currentExpiresAt: Instant,
        @Param("newExpiresAt") newExpiresAt: Instant,
    ): Long
}
