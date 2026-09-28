package io.klibs.core.user.service

import io.klibs.core.user.model.AuthenticationProvider
import io.klibs.core.user.model.ExternalUserIdentity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AuthenticationHashingServiceTest {

    @Test
    fun `hashes session tokens using the expected HMAC`() {
        val service = AuthenticationHashingService(SECRET.copyOf())

        assertEquals(
            "c99f455fb84f6c109473b11de7df05f2053d9a09dcafd6b38ee45e50d60e0be6",
            service.hashSessionToken("test-token"),
        )
    }

    @Test
    fun `hashes external identities using the provider and a separate domain`() {
        val service = AuthenticationHashingService(SECRET.copyOf())
        val identity = ExternalUserIdentity(
            provider = AuthenticationProvider.JETBRAINS_HUB,
            externalUserId = "hub-user-42",
        )

        val identityHash = service.hashExternalIdentity(identity)

        assertEquals(
            "c4b1cdb889fc26daf61e1a233d54e4f8a4d9a0e8ede3a6318fcf9b5c34b23298",
            identityHash,
        )
        assertNotEquals(service.hashSessionToken(identity.externalUserId), identityHash)
    }

    @Test
    fun `copies the secret passed by the caller`() {
        val mutableSecret = SECRET.copyOf()
        val service = AuthenticationHashingService(mutableSecret)
        mutableSecret.fill(0)

        assertEquals(
            "c99f455fb84f6c109473b11de7df05f2053d9a09dcafd6b38ee45e50d60e0be6",
            service.hashSessionToken("test-token"),
        )
    }

    @Test
    fun `rejects an empty secret`() {
        assertFailsWith<IllegalArgumentException> {
            AuthenticationHashingService(byteArrayOf())
        }
    }

    private companion object {
        val SECRET = "0123456789abcdef0123456789abcdef".toByteArray()
    }
}
