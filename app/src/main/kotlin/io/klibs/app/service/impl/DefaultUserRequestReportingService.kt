package io.klibs.app.service.impl

import io.klibs.app.configuration.UserRequestReportingConfiguration
import io.klibs.app.dto.UserRequestReport
import io.klibs.app.service.UserIssueNotifier
import io.klibs.app.service.UserRequestReportQueue
import io.klibs.app.service.UserRequestReportingService
import io.klibs.core.pckg.entity.PackageIndexKey
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.UserRequestIndexingStatus
import io.klibs.core.pckg.enums.UserRequestProcessingStatus
import io.klibs.core.pckg.repository.PackageIndexRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class DefaultUserRequestReportingService(
    private val userRequestReportQueue: UserRequestReportQueue,
    private val userRequestIssueRepository: UserRequestIssueRepository,
    private val userIssueNotifier: UserIssueNotifier,
    private val packageIndexRepository: PackageIndexRepository,
    private val reportingConfiguration: UserRequestReportingConfiguration,
) : UserRequestReportingService {

    override fun processReportsQueue(): Boolean {
        val report = userRequestReportQueue.poll()
        if (report == null) {
            logger.debug("The user request report queue is empty")
            return false
        }
        try {
            processReport(report)
        } catch (e: Exception) {
            logger.error("Failed to publish user request report ${report.reportId}: ${e.message}", e)
            try {
                userRequestReportQueue.markAsFailed(report, e.message)
            } catch (ex: Exception) {
                logger.error("Failed to mark report ${report.reportId} as failed: ${ex.message}", ex)
            }
        }
        return true
    }

    private fun processReport(report: UserRequestReport) {
        val issue = userRequestIssueRepository.findById(report.userRequestIssueId).getOrNull()
        if (issue == null) {
            logger.error("User request issue ${report.userRequestIssueId} not found for report ${report.reportId}")
            userRequestReportQueue.markAsFailed(report, "User request issue not found")
            return
        }

        when (issue.processingStatus) {
            // The issue already reached a final state, so its result was published before; drop the report.
            UserRequestProcessingStatus.PROCESSED -> userRequestReportQueue.markAsSuccess(report)

            UserRequestProcessingStatus.ACCEPTED, UserRequestProcessingStatus.FAILED -> evaluate(report, issue)

            UserRequestProcessingStatus.NEW, UserRequestProcessingStatus.REJECTED -> {
                logger.error("Unexpected status ${issue.processingStatus} of issue ${issue.id} for report ${report.reportId}")
                userRequestReportQueue.markAsFailed(report, "Unexpected issue status ${issue.processingStatus}")
            }
        }
    }

    private fun evaluate(report: UserRequestReport, issue: UserRequestIssueEntity) {
        when (report.indexingStatus) {
            UserRequestIndexingStatus.SUCCESS -> {
                if (packageIndexRepository.existsById(PackageIndexKey(report.groupId, report.artifactId))) {
                    publishSuccess(report, issue)
                } else {
                    logger.trace(
                        "Package ${report.groupId}:${report.artifactId} not indexed yet; " +
                            "deferring report ${report.reportId} for ${reportingConfiguration.indexDefer}"
                    )
                    userRequestReportQueue.deferFor(report, reportingConfiguration.indexDefer)
                }
            }

            // A failure was already reported for a FAILED issue, so we don't repeat a failure comment.
            UserRequestIndexingStatus.FAILURE -> {
                if (issue.processingStatus == UserRequestProcessingStatus.FAILED) {
                    userRequestReportQueue.markAsSuccess(report)
                } else {
                    publishFailure(report, issue)
                }
            }
        }
    }

    private fun publishSuccess(report: UserRequestReport, issue: UserRequestIssueEntity) {
        userIssueNotifier.notifyIndexingSuccess(issue.githubIssueNumber)
        completeReport(report, issue, UserRequestProcessingStatus.PROCESSED, statusDetails = null)
    }

    private fun publishFailure(report: UserRequestReport, issue: UserRequestIssueEntity) {
        userIssueNotifier.notifyIndexingFailure(issue.githubIssueNumber, report.statusDetails)
        completeReport(report, issue, UserRequestProcessingStatus.FAILED, statusDetails = report.statusDetails)
    }

    private fun completeReport(
        report: UserRequestReport,
        issue: UserRequestIssueEntity,
        newStatus: UserRequestProcessingStatus,
        statusDetails: String?,
    ) {
        userRequestIssueRepository.save(issue.copy(processingStatus = newStatus, statusDetails = statusDetails))
        userRequestReportQueue.markAsSuccess(report)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultUserRequestReportingService::class.java)
    }
}
