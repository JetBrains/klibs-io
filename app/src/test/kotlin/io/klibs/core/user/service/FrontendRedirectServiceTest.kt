package io.klibs.core.user.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class FrontendRedirectServiceTest {
    private val uut = FrontendRedirectService()

    @Test
    fun `missing returnTo becomes the frontend root`() {
        assertEquals("/", uut.sanitizeReturnTo(null))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "/project/JetBrains/kotlin", "/search?q=C%2B%2B", "/search?q=kotlin+http#results"])
    fun `preserves a safe frontend path and its query encoding`(returnTo: String) {
        assertEquals(returnTo, uut.sanitizeReturnTo(returnTo))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "https://evil.example", "//evil.example", "///evil.example",
            "https://frontend.example.evil.example/", "javascript:alert(1)", "project/kotlin",
            "/\\evil.example", "/%5Cevil.example", "/%2Fevil.example", "%2F%2Fevil.example",
            "/path\r\nLocation:https://evil.example", "/%0d%0aLocation:evil", "/%00", "/%7f",
            "/bad%", "/bad%GG", "/invalid path",
        ]
    )
    fun `unsafe returnTo becomes the frontend root`(returnTo: String) {
        assertEquals("/", uut.sanitizeReturnTo(returnTo))
    }

    @Test
    fun `encodes Unicode in a sanitized returnTo`() {
        assertEquals("/%D0%BA", uut.sanitizeReturnTo("/к"))
    }

    @Test
    fun `overlong returnTo becomes the frontend root`() {
        assertEquals("/", uut.sanitizeReturnTo("/" + "a".repeat(1024)))
    }
}
