package io.klibs.core.pckg.entity

import io.klibs.core.pckg.enums.PackageIndexingErrorType

import io.klibs.integration.maven.ScraperType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

/**
 * We store in this table GAV coordinates for which we couldn't determine whether they are KMP packages or not.
 * It could be KMP package with broken tooling metadata json file or old versions of KMP package, before it became KMP.
 */
@Entity
@Table(name = "rejected_maven_coordinate")
data class RejectedMavenCoordinateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rejected_maven_coordinate_id_seq")
    @SequenceGenerator(name = "rejected_maven_coordinate_id_seq", sequenceName = "rejected_maven_coordinate_id_seq")
    @Column(name = "id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maven_artifact_id", nullable = false)
    val mavenArtifact: MavenArtifactEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "scraper_type", nullable = false)
    val repo: ScraperType,

    @Column(name = "scm_url")
    val scmUrl: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false)
    val errorType: PackageIndexingErrorType,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
