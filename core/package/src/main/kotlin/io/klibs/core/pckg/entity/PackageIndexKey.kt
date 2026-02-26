package io.klibs.core.pckg.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class PackageIndexKey(
    @Column(name = "group_id")
    val groupId: String,

    @Column(name = "artifact_id")
    val artifactId: String,
) : Serializable
