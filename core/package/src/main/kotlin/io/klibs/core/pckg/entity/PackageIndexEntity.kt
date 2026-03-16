package io.klibs.core.pckg.entity

import io.klibs.core.pckg.model.PackagePlatform
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Immutable
@Table(name = "package_index")
class PackageIndexEntity(
    @EmbeddedId
    val id: PackageIndexKey,

    @Column(name = "project_id")
    val projectId: Int?,

    @Column(name = "latest_package_id")
    var latestPackageId: Long,

    @Column(name = "latest_version")
    val latestVersion: String,

    @Column(name = "latest_description")
    val latestDescription: String?,

    @Column(name = "release_ts")
    val releaseTs: Instant,

    @Column(name = "owner_type")
    val ownerType: String,

    @Column(name = "owner_login")
    val ownerLogin: String,

    @Column(name = "latest_license_name")
    val latestLicenseName: String?,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "platforms")
    val platforms: List<PackagePlatform>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "targets")
    val targets: Map<PackagePlatform, List<String>>
)