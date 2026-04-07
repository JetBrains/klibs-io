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

    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: Instant = Instant.EPOCH,

    @Column(name = "last_processed_at", nullable = false)
    var lastProcessedAt: Instant = Instant.EPOCH
)