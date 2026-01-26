package io.klibs.core.pckg.entity

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import jakarta.persistence.*
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

    @Column(name = "targets", columnDefinition = "text[]")
    val targets: Array<String>
) {
    @get:Transient
    val parsedTargets: List<PackageTarget>
        get() {
            val existingTargets = targets.map { raw ->
                parseTargetOrThrow(raw)
            }

            val platformsInTargets = existingTargets.map { it.platform }.toSet()

            val missingTargets = platforms
                .filter { it !in platformsInTargets }
                .map { platform -> PackageTarget(platform = platform, target = null) }

            return existingTargets + missingTargets
        }

    private fun parseTargetOrThrow(raw: String): PackageTarget {
        val parts = raw.split('_', limit = 2)
        require(parts.size == 2) {
            "Invalid package_index.targets entry '$raw' for ${id.groupId}:${id.artifactId}. " +
                    "Expected format '<PLATFORM>_<TARGET>'."
        }

        val platformRaw = parts[0].trim()
        val targetRaw = parts[1].trim()

        val platform = try {
            PackagePlatform.valueOf(platformRaw)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid platform '$platformRaw' in package_index.targets entry '$raw' for ${id.groupId}:${id.artifactId}. " +
                        "Expected one of: ${PackagePlatform.entries.joinToString { it.name }}",
                e
            )
        }

        return PackageTarget(
            platform = platform,
            target = targetRaw.ifBlank { null }
        )
    }
}