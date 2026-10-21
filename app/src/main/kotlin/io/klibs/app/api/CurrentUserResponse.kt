package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CurrentUserResponse(
    @field:Schema(
        description = "Whether the request has a valid klibs.io user session",
        example = "true",
    )
    val authenticated: Boolean,

    @field:Schema(
        description = "Local klibs.io user ID; absent when the request is unauthenticated",
        example = "11111111-1111-1111-1111-111111111111",
    )
    val userId: UUID?,
)
