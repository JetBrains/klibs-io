package io.klibs.core.pckg.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "banned_packages")
data class BannedPackagesEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "group_id", nullable = false)
    val groupId: String,

    @Column(name = "artifact_id")
    val artifactId: String?,

    @Column(name = "reason")
    val reason: String?,
)
