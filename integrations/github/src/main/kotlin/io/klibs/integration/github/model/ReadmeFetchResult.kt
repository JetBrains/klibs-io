package io.klibs.integration.github.model

sealed class ReadmeFetchResult {
    data class Content(val markdown: String) : ReadmeFetchResult()

    data object NotModified : ReadmeFetchResult()

    data object NotFound : ReadmeFetchResult()

    data class Error(val status: Int? = null, val message: String? = null) : ReadmeFetchResult()
}
