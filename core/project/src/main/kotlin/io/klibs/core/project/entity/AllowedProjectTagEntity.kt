package io.klibs.core.project.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "allowed_project_tags")
data class AllowedProjectTagEntity(
    @Id
    @Column(name = "name")
    val name: String,

    @Column(name = "definition")
    val definition: String?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positive_cues", columnDefinition = "jsonb")
    val positiveCues: List<String> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "negative_cues", columnDefinition = "jsonb")
    val negativesCues: List<String> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "synonyms", columnDefinition = "jsonb")
    val synonyms: List<String> = emptyList(),
)