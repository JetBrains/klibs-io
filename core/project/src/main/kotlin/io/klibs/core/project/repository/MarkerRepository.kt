package io.klibs.core.project.repository

import io.klibs.core.project.entity.Marker
import io.klibs.core.project.entity.MarkerKey
import org.springframework.data.repository.CrudRepository

interface MarkerRepository : CrudRepository<Marker, MarkerKey> {
    fun findAllByProjectId(projectId: Int): List<Marker>
}