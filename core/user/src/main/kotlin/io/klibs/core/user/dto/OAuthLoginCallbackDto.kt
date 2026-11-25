package io.klibs.core.user.dto

/** Validated credentials for token exchange and the frontend destination after a successful login. */
class OAuthLoginCallbackDto(
    val codeVerifier: String,
    val redirectUrl: String,
)
