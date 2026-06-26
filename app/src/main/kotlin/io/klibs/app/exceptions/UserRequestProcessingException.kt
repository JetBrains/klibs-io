package io.klibs.app.exceptions

/**
 * Represents an exception that is thrown when a user request cannot be processed successfully.
 *
 * This exception is typically used to indicate specific reasons for the failure of user-related operations,
 * such as invalid or missing request data, unprocessable request parameters, or an already-processed state.
 *
 * @param reason A detailed message describing the reason for the exception.
 */
class UserRequestProcessingException(val reason: String) : RuntimeException(reason)