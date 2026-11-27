package io.klibs.core.user.service

import io.klibs.core.user.dto.OAuthLoginAttemptDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OAuthLoginAttemptServiceTest {
    private val settings = OAuthLoginSettings(Duration.ofMinutes(7), "https://frontend.example")
    private val hashingService = AuthenticationHashingService(SECRET)
    private val pkceService = PkceService()
    private val uut = serviceAt(NOW)

    @Test
    fun `accepts a signed attempt with the browser cookie and PKCE proof`() {
        val attempt = uut.createLoginAttempt("/search?q=C%2B%2B")

        val validated = assertNotNull(uut.validateLoginAttempt(attempt.state, attempt))

        assertEquals(attempt.codeVerifier, validated.codeVerifier)
        assertEquals("https://frontend.example/search?q=C%2B%2B", validated.redirectUrl)
        assertFalse(attempt.state.contains(attempt.codeVerifier))
    }

    @Test
    fun `independent logins have different states and PKCE credentials`() {
        val first = uut.createLoginAttempt("/")
        val second = uut.createLoginAttempt("/")

        assertNotEquals(first.state, second.state)
        assertNotEquals(first.codeVerifier, second.codeVerifier)
    }

    @Test
    fun `rejects a callback without a login cookie`() {
        val attempt = uut.createLoginAttempt("/")

        assertNull(uut.validateLoginAttempt(attempt.state, null))
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" ", "unrelated-state"])
    fun `rejects a callback without the matching state`(state: String?) {
        val attempt = uut.createLoginAttempt("/")

        assertFalse(uut.matchesBrowserAttempt(state, attempt))
        assertNull(uut.validateLoginAttempt(state, attempt))
    }

    @Test
    fun `another browser login cookie does not validate the callback`() {
        val first = uut.createLoginAttempt("/")
        val second = uut.createLoginAttempt("/")

        assertFalse(uut.matchesBrowserAttempt(first.state, second))
        assertNull(uut.validateLoginAttempt(first.state, second))
    }

    @Test
    fun `recognizes but rejects a matched state with a modified signature`() {
        val original = uut.createLoginAttempt("/")
        val signature = original.state.substringAfterLast('.')
        val replacement = if (signature.first() == '0') '1' else '0'
        val state = original.state.substringBeforeLast('.') + "." + replacement + signature.drop(1)
        val attempt = OAuthLoginAttemptDto(state, original.codeVerifier)

        assertTrue(uut.matchesBrowserAttempt(state, attempt))
        assertNull(uut.validateLoginAttempt(state, attempt))
    }

    @Test
    fun `rejects a modified returnTo even when state and cookie were modified together`() {
        val original = uut.createLoginAttempt("/project/kotlin")
        val parts = original.state.split('.').toMutableList()
        parts[2] = encode("//evil.example")
        val state = parts.joinToString(".")

        assertNull(uut.validateLoginAttempt(state, OAuthLoginAttemptDto(state, original.codeVerifier)))
    }

    @Test
    fun `rejects a different well-formed verifier before token exchange`() {
        val original = uut.createLoginAttempt("/")
        val changed = OAuthLoginAttemptDto(original.state, pkceService.createVerifier())

        assertNull(uut.validateLoginAttempt(original.state, changed))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "short", "invalid verifier"])
    fun `rejects a malformed verifier in a matching cookie`(verifier: String) {
        val original = uut.createLoginAttempt("/")

        assertNull(uut.validateLoginAttempt(original.state, OAuthLoginAttemptDto(original.state, verifier)))
    }

    @Test
    fun `accepts the attempt until just before its configured expiry`() {
        val attempt = uut.createLoginAttempt("/")
        val uut = serviceAt(NOW.plus(settings.attemptTtl).minusMillis(1))

        assertNotNull(uut.validateLoginAttempt(attempt.state, attempt))
    }

    @Test
    fun `recognizes but rejects the attempt exactly at its configured expiry`() {
        val attempt = uut.createLoginAttempt("/")
        val uut = serviceAt(NOW.plus(settings.attemptTtl))

        assertTrue(uut.matchesBrowserAttempt(attempt.state, attempt))
        assertNull(uut.validateLoginAttempt(attempt.state, attempt))
    }

    @Test
    fun `rejects an attempt after its configured expiry`() {
        val attempt = uut.createLoginAttempt("/")
        val uut = serviceAt(NOW.plus(settings.attemptTtl).plusSeconds(1))

        assertNull(uut.validateLoginAttempt(attempt.state, attempt))
    }

    @Test
    fun `rejects a state signed with a different secret`() {
        val attempt = uut.createLoginAttempt("/")
        val uut = serviceAt(NOW, AuthenticationHashingService(ByteArray(32) { 7 }))

        assertNull(uut.validateLoginAttempt(attempt.state, attempt))
    }

    @ParameterizedTest
    @ValueSource(strings = ["malformed", "1.two.three", "1.two.three.four.five.six"])
    fun `rejects malformed state in a matching cookie`(state: String) {
        assertNull(uut.validateLoginAttempt(state, OAuthLoginAttemptDto(state, pkceService.createVerifier())))
    }

    @Test
    fun `unsafe returnTo is replaced with the frontend root before signing`() {
        val attempt = uut.createLoginAttempt("//evil.example")

        val validated = assertNotNull(uut.validateLoginAttempt(attempt.state, attempt))
        assertEquals("https://frontend.example/", validated.redirectUrl)
    }

    private fun serviceAt(
        now: Instant,
        hashingService: AuthenticationHashingService = this.hashingService,
    ) = OAuthLoginAttemptService(
        hashingService,
        pkceService,
        FrontendRedirectService(),
        settings,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-09T10:00:00Z")
        val SECRET = "0123456789abcdef0123456789abcdef".toByteArray()
    }
}
