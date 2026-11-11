package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CurrentUserResponse(
    @field:Schema(
        description = "Local klibs.io user ID",
        example = "11111111-1111-1111-1111-111111111111",
    )
    val userId: UUID,
)
