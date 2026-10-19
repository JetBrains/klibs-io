package io.klibs.app.configuration

import io.klibs.app.service.CandidateBanRefusedException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(ex: MethodArgumentNotValidException): Map<String, String> {
        return ex.bindingResult.fieldErrors.associate { fieldError ->
            fieldError.field to (fieldError.defaultMessage ?: "Validation error")
        }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): Map<String, String> {
        return mapOf("error" to ex.message!!)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationExceptions(ex: ConstraintViolationException): ApiError {
        val errors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }

        return ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            message = "Validation failed",
            errors = errors
        )
    }

    @ExceptionHandler(CandidateBanRefusedException::class)
    fun handleCandidateBanRefused(e: CandidateBanRefusedException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body("${e.message}: the row is not PENDING, or the package is missing or already banned")
}

data class ApiError(
    val status: Int,
    val message: String,
    val errors: Map<String, String?>
)