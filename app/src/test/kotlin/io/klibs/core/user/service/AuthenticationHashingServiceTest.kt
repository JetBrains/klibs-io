package io.klibs.core.user.service

import io.klibs.core.user.model.ExternalUserIdentity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
    fun `hashes external identities using a separate domain`() {
        val service = AuthenticationHashingService(SECRET.copyOf())
        val identity = ExternalUserIdentity("hub-user-42")

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
    fun `signs OAuth state using the expected HMAC and a separate domain`() {
        val uut = AuthenticationHashingService(SECRET.copyOf())

        val signature = uut.signOAuthState("test-state")

        assertEquals("85364278dd537ed83aec782dd82a6d39b38b7c5bd52db814eca235cd23e52e48", signature)
        assertNotEquals(uut.hashSessionToken("test-state"), signature)
        assertNotEquals(uut.hashExternalIdentity(ExternalUserIdentity("test-state")), signature)
    }

    private companion object {
        val SECRET = "0123456789abcdef0123456789abcdef".toByteArray()
    }
}
