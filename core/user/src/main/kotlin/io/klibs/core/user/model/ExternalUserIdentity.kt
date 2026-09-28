package io.klibs.core.user.model

data class ExternalUserIdentity(
    val provider: AuthenticationProvider,
    val externalUserId: String,
) {
    init {
        require(externalUserId.isNotBlank()) { "External user id must not be blank" }
    }
}

enum class AuthenticationProvider {
    JETBRAINS_HUB
}
