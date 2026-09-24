package io.klibs.core.pckg.enums

/**
 * @property retryable whether indexing is retried with backoff before the coordinate is rejected.
 */
enum class PackageIndexingErrorType(val retryable: Boolean) {
    MISSING_TOOLING_METADATA(retryable = false),
    MISSING_POM(retryable = true),
    BROKEN_TOOLING_METADATA(retryable = false),
    BROKEN_POM(retryable = false),
}
