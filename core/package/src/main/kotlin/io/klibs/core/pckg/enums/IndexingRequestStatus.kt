package io.klibs.core.pckg.enums

enum class IndexingRequestStatus {
    PENDING,
    @Deprecated("IN_PROCESS status transition removed in favor of ShedLock single-worker mutual exclusion")
    IN_PROCESS,
}