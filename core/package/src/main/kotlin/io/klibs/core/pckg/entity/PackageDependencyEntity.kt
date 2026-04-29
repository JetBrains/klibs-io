package io.klibs.core.pckg.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

@Embeddable
data class PackageDependencyKey(
    @Column(name = "package_id")
    val packageId: Long,

    @Column(name = "dep_group_id")
    val depGroupId: String,

    @Column(name = "dep_artifact_id")
    val depArtifactId: String,

    @Column(name = "dep_version")
    val depVersion: String,
) : Serializable

@Entity
@Table(name = "package_dependency")
class PackageDependencyEntity(
    @EmbeddedId
    val id: PackageDependencyKey,
)
