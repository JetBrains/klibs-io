package io.klibs.core.pckg.enums

enum class PackageProcessingStatus(val description: String) {
    INDEXED("This package has been indexed and is available on klibs.io."),
    BANNED("This package has been banned and will not be available on klibs.io."),
    QUEUED("This package is queued for indexing. Please check its progress later."),
    FAILED("Indexing of this package failed and will not be retried."),
}
