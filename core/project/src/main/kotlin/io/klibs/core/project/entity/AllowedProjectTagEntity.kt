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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_synonyms", columnDefinition = "jsonb")
    val tagSynonyms: List<String> = emptyList(),
)