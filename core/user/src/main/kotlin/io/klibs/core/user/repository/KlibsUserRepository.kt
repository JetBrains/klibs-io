package io.klibs.core.user.repository

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.model.AuthenticationProvider
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface KlibsUserRepository : CrudRepository<UserEntity, UUID> {
    fun findByAuthenticationProviderAndExternalUserIdHash(
        authenticationProvider: AuthenticationProvider,
        externalUserIdHash: String,
    ): UserEntity?

    @Modifying
    @Query(
        """
            INSERT INTO UserEntity (id, authenticationProvider, externalUserIdHash)
            VALUES (:id, :authenticationProvider, :externalUserIdHash)
            ON CONFLICT(authenticationProvider, externalUserIdHash) DO NOTHING
        """
    )
    fun saveIfAbsent(
        @Param("id") id: UUID,
        @Param("authenticationProvider") authenticationProvider: AuthenticationProvider,
        @Param("externalUserIdHash") externalUserIdHash: String,
    ): Long
}
