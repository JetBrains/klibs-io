package io.klibs.core.user.service

import io.klibs.core.user.model.AuthenticationProvider
import io.klibs.core.user.model.ExternalUserIdentity
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AuthenticationHashingService(secret: ByteArray) {
    private val secret = secret.copyOf()

    init {
        require(secret.isNotEmpty()) { "HMAC secret must not be empty" }
    }

    fun hashSessionToken(token: String): String = computeHmac("$SESSION_TOKEN_PREFIX$token")

    fun hashExternalIdentity(identity: ExternalUserIdentity): String {
        val prefix = when (identity.provider) {
            AuthenticationProvider.JETBRAINS_HUB -> HUB_ACCOUNT_PREFIX
        }
        return computeHmac("$prefix${identity.externalUserId}")
    }

    private fun computeHmac(input: String): String {
        val hmac = Mac.getInstance(HMAC_ALGORITHM)
        hmac.init(SecretKeySpec(secret, HMAC_ALGORITHM))
        return hmac
            .doFinal(input.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
    }

    private companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
        const val SESSION_TOKEN_PREFIX = "session-token:"
        const val HUB_ACCOUNT_PREFIX = "hub-account:"
    }
}
