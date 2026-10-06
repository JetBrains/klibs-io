package io.klibs.core.user.service

import io.klibs.core.user.model.ExternalUserIdentity
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AuthenticationHashingService(secret: ByteArray) {
    private val secretKey = SecretKeySpec(secret, HMAC_ALGORITHM)

    fun hashSessionToken(token: String): String = computeHmac("$SESSION_TOKEN_PREFIX$token")

    fun hashExternalIdentity(identity: ExternalUserIdentity): String =
        computeHmac("$HUB_ACCOUNT_PREFIX${identity.externalUserId}")

    private fun computeHmac(input: String): String {
        val hmac = Mac.getInstance(HMAC_ALGORITHM)
        hmac.init(secretKey)
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
