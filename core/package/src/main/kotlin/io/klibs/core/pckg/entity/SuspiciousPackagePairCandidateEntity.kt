package io.klibs.core.pckg.entity

import io.klibs.core.pckg.enums.CandidateStatus
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
@Table(name = "suspicious_package_pair_candidate")
data class SuspiciousPackagePairCandidateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "suspicious_package_pair_candidate_id_seq")
    @SequenceGenerator(
        name = "suspicious_package_pair_candidate_id_seq",
        sequenceName = "suspicious_package_pair_candidate_id_seq",
    )
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "project_id")
    val projectId: Int,

    @Column(name = "artifact_id")
    val artifactId: String,

    @Column(name = "group_id")
    val groupId: String,

    @Column(name = "version_count")
    val versionCount: Int,

    @Column(name = "first_release_ts")
    val firstReleaseTs: Instant,

    @Column(name = "last_release_ts")
    val lastReleaseTs: Instant,

    @Column(name = "derived_owner_login")
    val derivedOwnerLogin: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    val status: CandidateStatus,

    @Column(name = "notes")
    val notes: String? = null,

    @Column(name = "detected_at")
    val detectedAt: Instant,
)
