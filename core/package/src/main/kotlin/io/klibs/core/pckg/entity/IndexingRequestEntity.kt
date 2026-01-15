package io.klibs.core.pckg.entity

import io.klibs.integration.maven.ScraperType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "package_index_request")
data class IndexingRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "package_index_request_id_seq")
    @SequenceGenerator(name = "package_index_request_id_seq", sequenceName = "package_index_request_id_seq", allocationSize = 50)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "group_id", nullable = false)
    val groupId: String,

    @Column(name = "artifact_id", nullable = false)
    val artifactId: String,

    @Column(name = "version")
    val version: String?,

    @Column(name = "released_ts")
    val releasedAt: Instant?,

    @Enumerated(EnumType.STRING)
    @Column(name = "scraper_type", nullable = false)
    val repo: ScraperType,

    @Column(name = "reindex", nullable = false)
    val reindex: Boolean = false,

    @Column(name = "failed_attempts", nullable = false)
    val failedAttempts: Int = 0,

    @Column(name = "failed_ts")
    val failedTs: Instant? = null,

    @Column(name = "last_error_message")
    val lastErrorMessage: String? = null
) {
    val idNotNull: Long get() = requireNotNull(id)
}