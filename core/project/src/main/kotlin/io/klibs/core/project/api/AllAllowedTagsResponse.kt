package io.klibs.core.project.api

data class AllAllowedTagsResponse(
    val tags: List<AllowedTagResponseDto>,
) {
    data class AllowedTagResponseDto(
        val name: String,
        val definition: String?,
    )
}