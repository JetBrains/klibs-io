package io.klibs.app.service.impl

import io.klibs.app.dto.UserRequestReport
import io.klibs.app.service.UserRequestReportQueue
import io.klibs.core.pckg.entity.UserRequestReportEntity
import io.klibs.core.pckg.repository.UserRequestReportRepository
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class DatabaseUserRequestReportQueue(
    private val userRequestReportRepository: UserRequestReportRepository,
) : UserRequestReportQueue {

    override fun poll(): UserRequestReport? =
        userRequestReportRepository.findFirstForReportingBefore(Instant.now(), Limit.of(1))?.toReport()

    override fun deferFor(report: UserRequestReport, delay: Duration) {
        userRequestReportRepository.deferUntil(report.reportId, Instant.now().plus(delay))
    }

    override fun markAsSuccess(report: UserRequestReport) {
        userRequestReportRepository.deleteById(report.reportId)
    }

    override fun markAsFailed(report: UserRequestReport, errorMessage: String?) {
        userRequestReportRepository.markAsFailed(report.reportId, errorMessage)
    }

    private fun UserRequestReportEntity.toReport() = UserRequestReport(
        reportId = requireNotNull(id) {
            "UserRequestReportEntity.id is null — entity was not persisted"
        },
        userRequestIssueId = requireNotNull(userRequestIssue.id) {
            "UserRequestReportEntity.userRequestIssue.id is null — entity was not persisted"
        },
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        indexingStatus = indexingStatus,
        statusDetails = statusDetails,
    )
}
