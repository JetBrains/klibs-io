package io.klibs.app.indexing.discoverer

import io.klibs.integration.maven.MavenArtifact
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow


interface PackageDiscoverer {

    suspend fun discover(
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact>
}