package io.klibs.core.user.entity

import io.klibs.core.user.model.AuthenticationProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "klibs_user")
data class UserEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_provider", nullable = false)
    val authenticationProvider: AuthenticationProvider,

    @Column(name = "external_user_id_hash", nullable = false)
    val externalUserIdHash: String,
)
