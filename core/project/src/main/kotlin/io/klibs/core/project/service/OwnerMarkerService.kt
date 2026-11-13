package io.klibs.core.project.service

import io.klibs.core.project.entity.Marker
import io.klibs.core.project.enums.MarkerType
import io.klibs.core.project.repository.MarkerRepository
import org.springframework.stereotype.Service

@Service
class OwnerMarkerService(
    private val markerRepository: MarkerRepository,
) {

    fun assignOwnerMarkers(projectId: Int, ownerLogin: String) {
        val markers = OWNER_MARKERS
            .filterValues { owners -> owners.any { it.equals(ownerLogin, ignoreCase = true) } }
            .keys
            .map { Marker(projectId = projectId, type = it) }

        if (markers.isNotEmpty()) {
            markerRepository.saveAll(markers)
        }
    }

    companion object {
        val OWNER_MARKERS: Map<MarkerType, Set<String>> = mapOf(
            MarkerType.JETBRAINS_AND_GOOGLE to setOf("Kotlin", "JetBrains", "androidx"),
        )
    }
}
