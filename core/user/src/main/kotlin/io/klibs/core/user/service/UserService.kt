package io.klibs.core.user.service

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.repository.KlibsUserRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class UserService(
    private val userRepository: KlibsUserRepository,
    private val hashingService: AuthenticationHashingService,
) {
    fun findOrCreate(identity: ExternalUserIdentity): UserEntity {
        val externalUserIdHash = hashingService.hashExternalIdentity(identity)

        val existingUser = userRepository.findByAuthenticationProviderAndExternalUserIdHash(
            identity.provider,
            externalUserIdHash,
        )
        if (existingUser != null) return existingUser

        userRepository.saveIfAbsent(
            UUID.randomUUID(),
            identity.provider,
            externalUserIdHash,
        )

        val createdUser = userRepository.findByAuthenticationProviderAndExternalUserIdHash(
            identity.provider,
            externalUserIdHash,
        )
        if (createdUser == null) {
            throw IllegalStateException("User is still missing after insert")
        }

        return createdUser
    }
}
