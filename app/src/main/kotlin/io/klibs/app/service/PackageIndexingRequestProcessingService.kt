package io.klibs.app.service

interface PackageIndexingRequestProcessingService<T> {
    fun processRequest(request: T)
}
