package io.klibs.app.indexing.discoverer

import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow


interface PackageDiscoverer {

    suspend fun discover(
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact>
}

fun collectAllKnownMavenCentralPackages(packageRepository: PackageRepository): Map<String, Set<String>> = packageRepository.findAllKnownMavenCentralPackages()
    .associateBy({ createArtifactCoordinates(it.groupId, it.artifactId) }) { it.versions }

fun createArtifactCoordinates(groupId: String, artifactId: String): String = "$groupId:$artifactId"
