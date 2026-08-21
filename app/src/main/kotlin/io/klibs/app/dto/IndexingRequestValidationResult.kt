package io.klibs.app.dto

import org.springframework.http.ResponseEntity

sealed class IndexingRequestValidationResult<out P> {
    data class Valid<T>(val request: T) : IndexingRequestValidationResult<T>()
    data class NotApplicable(val response: ResponseEntity<Void>) : IndexingRequestValidationResult<Nothing>()
}
