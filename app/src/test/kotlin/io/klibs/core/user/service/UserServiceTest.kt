package io.klibs.core.user.service

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.model.AuthenticationProvider
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.repository.KlibsUserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class UserServiceTest {
    private val userRepository = mock<KlibsUserRepository>()
    private val hashingService = mock<AuthenticationHashingService>()
    private val service = UserService(userRepository, hashingService)

    @Test
    fun `returns an existing user without trying to insert`() {
        val existingUser = user()
        whenever(hashingService.hashExternalIdentity(IDENTITY)).thenReturn(EXTERNAL_USER_ID_HASH)
        whenever(
            userRepository.findByAuthenticationProviderAndExternalUserIdHash(
                IDENTITY.provider,
                EXTERNAL_USER_ID_HASH,
            )
        ).thenReturn(existingUser)

        val result = service.findOrCreate(IDENTITY)

        assertSame(existingUser, result)
        verify(userRepository, never()).saveIfAbsent(any(), any(), any())
    }

    @Test
    fun `returns the stored user after an idempotent insert`() {
        val storedUser = user()
        whenever(hashingService.hashExternalIdentity(IDENTITY)).thenReturn(EXTERNAL_USER_ID_HASH)
        whenever(
            userRepository.findByAuthenticationProviderAndExternalUserIdHash(
                IDENTITY.provider,
                EXTERNAL_USER_ID_HASH,
            )
        ).thenReturn(null, storedUser)

        val result = service.findOrCreate(IDENTITY)

        assertSame(storedUser, result)
        verify(userRepository).saveIfAbsent(
            any(),
            eq(IDENTITY.provider),
            eq(EXTERNAL_USER_ID_HASH),
        )
    }

    @Test
    fun `fails when the user is still missing after an insert attempt`() {
        whenever(hashingService.hashExternalIdentity(IDENTITY)).thenReturn(EXTERNAL_USER_ID_HASH)
        whenever(
            userRepository.findByAuthenticationProviderAndExternalUserIdHash(
                IDENTITY.provider,
                EXTERNAL_USER_ID_HASH,
            )
        ).thenReturn(null)

        assertFailsWith<IllegalStateException> {
            service.findOrCreate(IDENTITY)
        }
    }

    private fun user() = UserEntity(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        authenticationProvider = IDENTITY.provider,
        externalUserIdHash = EXTERNAL_USER_ID_HASH,
    )

    private companion object {
        val IDENTITY = ExternalUserIdentity(AuthenticationProvider.JETBRAINS_HUB, "hub-user-42")
        const val EXTERNAL_USER_ID_HASH = "external-user-id-hash"
    }
}
