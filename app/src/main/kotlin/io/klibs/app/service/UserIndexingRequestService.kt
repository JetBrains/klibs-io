package io.klibs.app.service

import java.util.UUID

interface UserIndexingRequestService {
    /**
     * Processing user's request: discovers and saves packages for indexing
     */
    fun discoverAndSaveRequest(userRequestId: UUID)

    /**
     * Saves packages for a GAV request without running the discovery
     */
    fun saveGAVRequest(groupId: String, artifactId: String, version: String)
}
