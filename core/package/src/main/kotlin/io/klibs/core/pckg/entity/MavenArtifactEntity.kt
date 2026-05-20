package io.klibs.core.pckg.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "maven_artifact")
data class MavenArtifactEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maven_artifact_id_seq")
    @SequenceGenerator(name = "maven_artifact_id_seq", sequenceName = "maven_artifact_id_seq")
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "group_id")
    val groupId: String,

    @Column(name = "artifact_id")
    val artifactId: String,

    @Column(name = "version")
    val version: String,
)
