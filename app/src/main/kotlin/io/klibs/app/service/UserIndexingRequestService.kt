package io.klibs.app.service

import java.util.UUID

interface UserIndexingRequestService {
    /**
     * Processing user's request: discovers and saves packages for indexing
     */
    fun fulfillRequest(userRequestId: UUID)
}
