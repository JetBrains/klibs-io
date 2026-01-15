package io.klibs.core.pckg.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "OptionalLink",
    description = "An optional link. The title is always present, but the url is not guaranteed to be there"
)
data class OptionalLinkResponse(
    @Schema(
        description = "Title of the link. Always present",
        example = "GitHub Issues"
    )
    val title: String,

    @Schema(
        description = "URL of the link. May be null",
        example = "https://github.com/kotlin/dokka"
    )
    val url: String?
)
