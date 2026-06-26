package io.klibs.core.pckg.entity

import io.klibs.core.pckg.enums.UserRequestProcessingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_request_issue")
data class UserRequestIssueEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    val id: UUID? = null,

    @Column(name = "github_issue_number", nullable = false)
    val githubIssueNumber: Int,

    @Column(name = "group_id", nullable = false)
    val groupId: String,

    @Column(name = "artifact_id", nullable = false)
    val artifactId: String,

    @Column(name = "version")
    val version: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    val processingStatus: UserRequestProcessingStatus = UserRequestProcessingStatus.NEW,

    @Column(name = "status_details")
    val statusDetails: String? = null,

    @Column(name = "failed_attempts", nullable = false)
    val failedAttempts: Int = 0,

    @Column(name = "failed_ts")
    val failedTs: Instant? = null,

    @Column(name = "last_error_message")
    val lastErrorMessage: String? = null
)