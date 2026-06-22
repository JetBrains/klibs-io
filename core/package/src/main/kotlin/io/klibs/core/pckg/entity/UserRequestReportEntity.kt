package io.klibs.core.pckg.entity

import io.klibs.core.pckg.enums.UserRequestIndexingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "user_request_report")
data class UserRequestReportEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_request_issue_id", nullable = false)
    val userRequestIssue: UserRequestIssueEntity,

    @Column(name = "group_id", nullable = false)
    val groupId: String,

    @Column(name = "artifact_id", nullable = false)
    val artifactId: String,

    @Column(name = "version")
    val version: String?,

    /**
     * The final result of the indexing process.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false)
    val indexingStatus: UserRequestIndexingStatus,

    /**
     * Additional information regarding the indexing result, such as error messages.
     */
    @Column(name = "status_details")
    val statusDetails: String? = null,

    @Column(name = "failed_attempts", nullable = false)
    val failedAttempts: Int = 0,

    @Column(name = "failed_ts")
    val failedTs: Instant? = null,

    @Column(name = "last_error_message")
    val lastErrorMessage: String? = null,
)