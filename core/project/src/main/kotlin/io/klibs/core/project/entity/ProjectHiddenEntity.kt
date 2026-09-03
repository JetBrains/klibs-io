package io.klibs.core.project.entity

import io.klibs.core.project.enums.HideOrigin
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * One row per hidden project; a project with no row is visible.
 */
@Entity
@Table(name = "project_hidden")
data class ProjectHiddenEntity(

    @Id
    @Column(name = "project_id")
    val projectId: Int,

    @Column(name = "hidden_at")
    val hiddenAt: Instant,

    @Column(name = "origin")
    @Enumerated(EnumType.STRING)
    val origin: HideOrigin,

    @Column(name = "reason")
    val reason: String?,
)
