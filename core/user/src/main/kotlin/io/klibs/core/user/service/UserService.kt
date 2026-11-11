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
        val hash = hashingService.hashExternalIdentity(identity)
        userRepository.findByExternalUserIdHash(hash)?.let { return it }
        userRepository.saveIfAbsent(id = UUID.randomUUID(), externalUserIdHash = hash)
        return checkNotNull(userRepository.findByExternalUserIdHash(hash)) {
            "User is still missing after insert"
        }
    }
}
