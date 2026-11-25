package io.klibs.core.user.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PkceServiceTest {
    private val uut = PkceService()

    @Test
    fun `generates a URL-safe verifier with 256 bits of randomness`() {
        val verifier = uut.createVerifier()

        assertEquals(43, verifier.length)
        assertTrue(Regex("[A-Za-z0-9_-]{43}").matches(verifier))
        assertNotEquals(verifier, uut.createVerifier())
    }

    @Test
    fun `calculates the S256 challenge from the RFC 7636 example`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            uut.createChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }
}
