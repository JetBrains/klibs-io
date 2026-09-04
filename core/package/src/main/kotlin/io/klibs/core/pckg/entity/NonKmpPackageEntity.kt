package io.klibs.core.pckg.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "non_kmp_packages")
data class NonKmpPackageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "non_kmp_packages_id_seq")
    @SequenceGenerator(name = "non_kmp_packages_id_seq", sequenceName = "non_kmp_packages_id_seq", allocationSize = 50)
    @Column(name = "id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maven_artifact_id", nullable = false)
    val mavenArtifact: MavenArtifactEntity,
)
