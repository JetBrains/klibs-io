package io.klibs.core.user.model

data class ExternalUserIdentity(
    val externalUserId: String,
) {
    init {
        require(externalUserId.isNotBlank()) { "External user id must not be blank" }
    }
}
