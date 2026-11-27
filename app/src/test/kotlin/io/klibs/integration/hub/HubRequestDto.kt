package io.klibs.integration.hub

class HubRequestDto(
    val method: String,
    val path: String,
    val authorization: String?,
    val contentType: String?,
    val body: String,
)
