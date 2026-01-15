package io.klibs.core.project.entity

import io.klibs.core.project.enums.TagOrigin
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table

/**
 * JPA entity for the project_tags table.
 *
 * The table has a uniqueness constraint on (project_id, origin, value). We model that
 * as a composite identifier using @IdClass so Spring Data can work with it.
 */
@Entity
@Table(name = "project_tags")
@IdClass(TagKey::class)
data class TagEntity(
    @Id
    @Column(name = "project_id")
    val projectId: Int,

    @Id
    @Column(name = "origin")
    @Enumerated(EnumType.STRING)
    val origin: TagOrigin,

    @Id
    @Column(name = "value")
    val value: String,
)

/**
 * Composite identifier for TagEntity.
 */
data class TagKey(
    val projectId: Int? = null,
    val origin: TagOrigin? = null,
    val value: String? = null,
)