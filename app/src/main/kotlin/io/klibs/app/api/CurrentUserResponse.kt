package io.klibs.app.api

import java.util.UUID

data class CurrentUserResponse(
    val authenticated: Boolean,
    val userId: UUID?,
)
