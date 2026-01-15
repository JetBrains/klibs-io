package io.klibs.core.pckg.entity

import io.klibs.core.pckg.model.PackagePlatform
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "package_target")
data class PackageTargetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "package_target_id_seq")
    @SequenceGenerator(name = "package_target_id_seq", sequenceName = "package_target_id_seq", allocationSize = 50)
    @Column(name = "id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    var packageEntity: PackageEntity? = null,

    @Column(name = "platform")
    @Enumerated(EnumType.STRING)
    val platform: PackagePlatform,

    @Column(name = "target")
    val target: String?
)
