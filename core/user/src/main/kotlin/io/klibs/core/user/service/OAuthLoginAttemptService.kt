package io.klibs.core.user.service

import io.klibs.core.user.dto.OAuthLoginAttemptDto
import io.klibs.core.user.dto.OAuthLoginCallbackDto
import io.klibs.integration.hub.configuration.properties.HubAuthenticationProperties.Companion.ENABLED_PROPERTY
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64

@Service
@ConditionalOnProperty(ENABLED_PROPERTY, havingValue = "true")
class OAuthLoginAttemptService(
    private val hashingService: AuthenticationHashingService,
    private val pkceService: PkceService,
    private val frontendRedirectService: FrontendRedirectService,
    private val settings: OAuthLoginSettings,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun createLoginAttempt(returnTo: String?): OAuthLoginAttemptDto {
        val codeVerifier = pkceService.createVerifier()
        val expiresAt = clock.instant().plus(settings.attemptTtl).epochSecond
        val nonce = ByteArray(NONCE_SIZE_BYTES).also(secureRandom::nextBytes)
        val encodedReturnTo = encode(frontendRedirectService.sanitizeReturnTo(returnTo).toByteArray(Charsets.UTF_8))
        // The public challenge binds the cookie's secret verifier without exposing it in URL-visible state.
        val payload = "$expiresAt.${encode(nonce)}.$encodedReturnTo.${pkceService.createChallenge(codeVerifier)}"
        return OAuthLoginAttemptDto("$payload.${hashingService.signOAuthState(payload)}", codeVerifier)
    }

    /** Establishes cookie ownership only; a matching but invalid/expired attempt still needs cookie cleanup. */
    fun matchesBrowserAttempt(state: String?, attempt: OAuthLoginAttemptDto?): Boolean =
        state != null && attempt != null && constantTimeEquals(state, attempt.state)

    fun validateLoginAttempt(state: String?, attempt: OAuthLoginAttemptDto?): OAuthLoginCallbackDto? {
        val credentials = attempt ?: return reject("missing_login_attempt")
        if (!matchesBrowserAttempt(state, credentials)) return reject("unmatched_state")
        val parts = credentials.state.split('.')
        if (parts.size != STATE_PARTS_COUNT) return reject("malformed_state")
        val (expiry, _, encodedReturnTo, challenge, signature) = parts
        val payload = credentials.state.substringBeforeLast('.')
        if (!constantTimeEquals(signature, hashingService.signOAuthState(payload))) return reject("invalid_signature")
        // A valid signature proves this service built the payload, so its fields need no format checks.
        if (expiry.toLong() <= clock.instant().epochSecond) return reject("expired_state")
        // The verifier comes from the unsigned cookie part, so it is still checked against the signed challenge.
        val actualChallenge = pkceService.createChallenge(credentials.codeVerifier)
        if (!constantTimeEquals(challenge, actualChallenge)) return reject("verifier_mismatch")
        val returnTo = String(Base64.getUrlDecoder().decode(encodedReturnTo), Charsets.UTF_8)
        return OAuthLoginCallbackDto(credentials.codeVerifier, settings.trustedFrontendOrigin + returnTo)
    }

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun constantTimeEquals(first: String, second: String): Boolean =
        MessageDigest.isEqual(first.toByteArray(Charsets.UTF_8), second.toByteArray(Charsets.UTF_8))

    private fun reject(category: String): OAuthLoginCallbackDto? {
        log.warn("Rejected OAuth login attempt: {}", category)
        return null
    }

    private companion object {
        val log = LoggerFactory.getLogger(OAuthLoginAttemptService::class.java)
        const val NONCE_SIZE_BYTES = 32
        const val STATE_PARTS_COUNT = 5
    }
}
