package io.klibs.app.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrustedOriginValidatorTest {

    @Test
    fun `accepts an exact origin match`() {
        val validator = TrustedOriginValidator(TRUSTED_ORIGIN)

        assertTrue(validator.isTrustedOrigin(request(TRUSTED_ORIGIN)))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://other.example",
            "https://frontend.example:443",
            "https://frontend.example/",
        ]
    )
    fun `rejects origins that are not an exact match`(origin: String) {
        val validator = TrustedOriginValidator(TRUSTED_ORIGIN)

        assertFalse(validator.isTrustedOrigin(request(origin)))
    }

    @Test
    fun `rejects a request without an origin`() {
        val validator = TrustedOriginValidator(TRUSTED_ORIGIN)

        assertFalse(validator.isTrustedOrigin(MockHttpServletRequest()))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://frontend.example/account?tab=security",
            "file://frontend.example",
        ]
    )
    fun `rejects invalid trusted origin configuration`(origin: String) {
        assertFailsWith<IllegalArgumentException> {
            TrustedOriginValidator(origin)
        }
    }

    @Test
    fun `requires https when requested by the caller`() {
        assertFailsWith<IllegalArgumentException> {
            TrustedOriginValidator("http://frontend.example", requireHttps = true)
        }

        val validator = TrustedOriginValidator(TRUSTED_ORIGIN, requireHttps = true)
        assertTrue(validator.isTrustedOrigin(request(TRUSTED_ORIGIN)))
    }

    private fun request(origin: String) = MockHttpServletRequest().apply {
        addHeader(HttpHeaders.ORIGIN, origin)
    }

    private companion object {
        const val TRUSTED_ORIGIN = "https://frontend.example"
    }
}
