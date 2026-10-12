package io.klibs.app.auth

import java.util.UUID

internal const val AUTHENTICATED_USER_AUTHORITY = "KLIBS_USER"

data class AuthenticatedUserPrincipal(
    val userId: UUID,
)
