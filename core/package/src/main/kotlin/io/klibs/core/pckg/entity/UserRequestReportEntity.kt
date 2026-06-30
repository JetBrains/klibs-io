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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "user_request_report")
data class UserRequestReportEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_request_report_id_seq")
    @SequenceGenerator(
        name = "user_request_report_id_seq",
        sequenceName = "user_request_report_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_request_issue_id", nullable = false)
    val userRequestIssue: UserRequestIssueEntity,

    @Column(name = "group_id", nullable = false)
    val groupId: String,

    @Column(name = "artifact_id", nullable = false)
    val artifactId: String,

    @Column(name = "version", nullable = false)
    val version: String,

    /**
     * The final result of the indexing process.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false)
    val indexingStatus: UserRequestIndexingStatus,

    /**
     * Why indexing ended this way (e.g. the failure reason), surfaced to the user in the GitHub issue.
     */
    @Column(name = "status_details")
    val statusDetails: String? = null,

    @Column(name = "failed_attempts", nullable = false)
    val failedAttempts: Int = 0,

    @Column(name = "failed_ts")
    val failedTs: Instant? = null,

    /**
     * Not visible for reporting until this moment; used to defer a report whose package is not indexed yet.
     */
    @Column(name = "next_attempt_ts")
    val nextAttemptTs: Instant? = null,

    /**
     * Why the last attempt to publish this report back to GitHub failed; unrelated to the indexing outcome.
     */
    @Column(name = "last_error_message")
    val lastErrorMessage: String? = null,
)