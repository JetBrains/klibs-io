package io.klibs.core.pckg.entity

import io.klibs.core.pckg.enums.CandidateStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "suspicious_package_candidate")
@IdClass(SuspiciousPackageCandidateKey::class)
data class SuspiciousPackageCandidateEntity(
    @Id
    @Column(name = "project_id")
    val projectId: Int,

    @Id
    @Column(name = "artifact_id")
    val artifactId: String,

    @Id
    @Column(name = "group_id")
    val groupId: String,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    val status: CandidateStatus,

    @Column(name = "notes")
    val notes: String? = null,

    @Column(name = "detected_at")
    val detectedAt: Instant,
)

data class SuspiciousPackageCandidateKey(
    val projectId: Int? = null,
    val artifactId: String? = null,
    val groupId: String? = null,
)
