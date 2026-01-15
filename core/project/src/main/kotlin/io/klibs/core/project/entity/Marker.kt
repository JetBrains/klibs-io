package io.klibs.core.project.entity

import io.klibs.core.project.enums.MarkerType
import jakarta.persistence.*

@Entity
@Table(name = "project_marker")
@IdClass(MarkerKey::class)
data class Marker(

    @Id
    @Column(name = "project_id")
    val projectId: Int,

    @Id
    @Column
    @Enumerated(value = EnumType.STRING)
    val type: MarkerType,
)

data class MarkerKey(val projectId: Int? = null, val type: MarkerType? = null)