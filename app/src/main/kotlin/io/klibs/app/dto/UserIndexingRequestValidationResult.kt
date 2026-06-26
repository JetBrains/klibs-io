package io.klibs.app.dto

import io.klibs.core.pckg.dto.UserIndexingRequestDto
import org.springframework.http.ResponseEntity

sealed class UserIndexingRequestValidationResult {
    data class Valid(val request: UserIndexingRequestDto) : UserIndexingRequestValidationResult()
    data class NotApplicable(val response: ResponseEntity<Void>) : UserIndexingRequestValidationResult()

    fun isValidRequest() = this is Valid
}