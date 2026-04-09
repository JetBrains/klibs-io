package io.klibs.core.readme.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "readme_metadata")
class ReadmeMetadataEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "scm_repo_id", unique = true)
    var scmRepoId: Int?,

    /**
     * Timestamp of the last successful call to the GitHub source repository
     * to check whether there is a new version of raw readme
     */
    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: Instant = Instant.EPOCH,

    /**
     * Timestamp of the last time the raw readme was processed.
     * That includes fetching the new HTML rendering and going through
     * all the processors.
     * In the implementation, whenever the lastProcessedAt is updated,
     * the lastSyncedAt is also updated. That's why lastProcessedAt can be either
     * equal, or older than lastSyncedAt.
     */
    @Column(name = "last_processed_at", nullable = false)
    var lastProcessedAt: Instant = Instant.EPOCH
)