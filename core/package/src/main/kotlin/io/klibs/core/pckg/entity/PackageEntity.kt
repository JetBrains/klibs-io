package io.klibs.core.pckg.entity


import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.integration.maven.ScraperType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "package")
data class PackageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "package_id_seq")
    @SequenceGenerator(name = "package_id_seq", sequenceName = "package_id_seq", allocationSize = 50)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "project_id")
    val projectId: Int?,

    @Enumerated(EnumType.STRING)
    @Column(name = "scraper_type")
    val repo: ScraperType,

    @Column(name = "group_id")
    val groupId: String,

    @Column(name = "artifact_id")
    val artifactId: String,

    @Column(name = "version")
    val version: String,

    @Column(name = "release_ts")
    val releaseTs: Instant,

    @Column(name = "name")
    val name: String,

    @Column(name = "description")
    val description: String?,

    @Column(name = "url")
    val url: String?,

    @Column(name = "scm_url")
    val scmUrl: String?,

    @Column(name = "build_tool")
    val buildTool: String,

    @Column(name = "build_tool_version")
    val buildToolVersion: String,

    @Column(name = "kotlin_version")
    val kotlinVersion: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "developers", columnDefinition = "jsonb")
    val developers: List<PackageDeveloper>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "licenses", columnDefinition = "jsonb")
    val licenses: List<PackageLicense>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb", nullable = true)
    val configuration: Configuration?,

    @Column(name = "generated_description", nullable = false)
    val generatedDescription: Boolean = false,
) {
    @OneToMany(mappedBy = "packageEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val targets: MutableList<PackageTargetEntity> = mutableListOf()

    val idNotNull: Long get() = requireNotNull(id)

    fun addTarget(target: PackageTargetEntity) {
        target.packageEntity = this
        targets.add(target)
    }

    /**
     * Creates a deep copy of this PackageEntity and reattaches all targets to the copy.
     * Allows specifying which properties should be changed in the copy, similar to Kotlin's copy() method.
     * 
     * @param id The ID of the new entity, defaults to the current entity's ID
     * @param projectId The project ID of the new entity, defaults to the current entity's project ID
     * @param repo The repository type of the new entity, defaults to the current entity's repository type
     * @param groupId The group ID of the new entity, defaults to the current entity's group ID
     * @param artifactId The artifact ID of the new entity, defaults to the current entity's artifact ID
     * @param version The version of the new entity, defaults to the current entity's version
     * @param releaseTs The release timestamp of the new entity, defaults to the current entity's release timestamp
     * @param name The name of the new entity, defaults to the current entity's name
     * @param description The description of the new entity, defaults to the current entity's description
     * @param url The URL of the new entity, defaults to the current entity's URL
     * @param scmUrl The SCM URL of the new entity, defaults to the current entity's SCM URL
     * @param buildTool The build tool of the new entity, defaults to the current entity's build tool
     * @param buildToolVersion The build tool version of the new entity, defaults to the current entity's build tool version
     * @param kotlinVersion The Kotlin version of the new entity, defaults to the current entity's Kotlin version
     * @param developers The developers of the new entity, defaults to the current entity's developers
     * @param licenses The licenses of the new entity, defaults to the current entity's licenses
     * @param configuration The configuration of the new entity, defaults to the current entity's configuration
     * @param generatedDescription Whether the description was generated, defaults to the current entity's value
     * @return A new PackageEntity instance with specified properties changed and targets reattached
     */
    fun deepCopy(
        id: Long? = this.id,
        projectId: Int? = this.projectId,
        repo: ScraperType = this.repo,
        groupId: String = this.groupId,
        artifactId: String = this.artifactId,
        version: String = this.version,
        releaseTs: Instant = this.releaseTs,
        name: String = this.name,
        description: String? = this.description,
        url: String? = this.url,
        scmUrl: String? = this.scmUrl,
        buildTool: String = this.buildTool,
        buildToolVersion: String = this.buildToolVersion,
        kotlinVersion: String = this.kotlinVersion,
        developers: List<PackageDeveloper> = this.developers,
        licenses: List<PackageLicense> = this.licenses,
        configuration: Configuration? = this.configuration,
        generatedDescription: Boolean = this.generatedDescription
    ): PackageEntity {
        // Create a copy of the entity with specified fields changed
        val copy = PackageEntity(
            id = id,
            projectId = projectId,
            repo = repo,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            releaseTs = releaseTs,
            name = name,
            description = description,
            url = url,
            scmUrl = scmUrl,
            buildTool = buildTool,
            buildToolVersion = buildToolVersion,
            kotlinVersion = kotlinVersion,
            developers = developers,
            licenses = licenses,
            configuration = configuration,
            generatedDescription = generatedDescription
        )

        // Create new copies of each target and attach them to the new entity
        this.targets.forEach { target ->
            val targetCopy = target.copy(packageEntity = null)
            copy.addTarget(targetCopy)
        }

        return copy
    }
}
