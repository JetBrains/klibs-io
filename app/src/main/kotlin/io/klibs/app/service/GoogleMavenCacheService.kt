package io.klibs.app.service

import io.klibs.integration.maven.androidx.GoogleMavenMasterIndexMetadata

interface GoogleMavenCacheService {
    fun readGroupIndexFromCache(groupId: String): GoogleMavenMasterIndexMetadata?
    fun writeGroupIndexToCache(groupId: String, content: String)
}