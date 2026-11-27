package io.klibs.core.user.dto

/** Browser-bound credentials kept together in the HttpOnly login cookie; the verifier must not enter a URL. */
class OAuthLoginAttemptDto(
    val state: String,
    val codeVerifier: String,
)
