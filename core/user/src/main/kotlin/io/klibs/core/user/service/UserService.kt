package io.klibs.core.user.service

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.repository.UserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val hashingService: AuthenticationHashingService,
) {
    fun findOrCreate(identity: ExternalUserIdentity): UserEntity {
        val externalUserIdHash = hashingService.hashExternalIdentity(identity)

        val existingUser = userRepository.findByExternalUserIdHash(externalUserIdHash)
        if (existingUser != null) return existingUser

        userRepository.saveIfAbsent(
            UUID.randomUUID(),
            externalUserIdHash,
        )

        val createdUser = userRepository.findByExternalUserIdHash(externalUserIdHash)
        if (createdUser == null) {
            throw IllegalStateException("User is still missing after insert")
        }

        return createdUser
    }
}
