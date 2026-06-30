package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.entity.UserRequestReportEntity
import io.klibs.core.pckg.enums.UserRequestIndexingStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Limit
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserRequestReportRepositoryTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var reportRepository: UserRequestReportRepository

    @Autowired
    private lateinit var issueRepository: UserRequestIssueRepository

    @Test
    fun `findFirstForReporting returns oldest report still within its retry budget`() {
        val issue = issueRepository.save(issue())
        reportRepository.save(report(issue, failedAttempts = 2))
        val oldestEligible = reportRepository.save(report(issue, failedAttempts = 0))
        reportRepository.save(report(issue, failedAttempts = 0))

        val found = reportRepository.findFirstForReportingBefore(Instant.now(), Limit.of(1))

        assertNotNull(found)
        assertEquals(oldestEligible.id, found.id, "Oldest report within retry budget should be returned")
    }

    @Test
    fun `findFirstForReporting skips a report deferred into the future and returns it once due`() {
        val issue = issueRepository.save(issue())
        val report = reportRepository.save(report(issue, failedAttempts = 0))

        reportRepository.deferUntil(requireNotNull(report.id), Instant.now().plus(1, ChronoUnit.HOURS))
        assertNull(
            reportRepository.findFirstForReportingBefore(Instant.now(), Limit.of(1)),
            "A report deferred into the future must be skipped"
        )

        reportRepository.deferUntil(requireNotNull(report.id), Instant.now().minus(1, ChronoUnit.MINUTES))
        assertEquals(report.id, reportRepository.findFirstForReportingBefore(Instant.now(), Limit.of(1))?.id)
    }

    @Test
    fun `markAsFailed increments attempts and stores the error message`() {
        val issue = issueRepository.save(issue())
        val report = reportRepository.save(report(issue, failedAttempts = 0))

        reportRepository.markAsFailed(requireNotNull(report.id), "test error")

        val reloaded = reportRepository.findById(requireNotNull(report.id)).get()
        assertEquals(1, reloaded.failedAttempts)
        assertEquals("test error", reloaded.lastErrorMessage)
        assertNotNull(reloaded.failedTs)
    }

    private fun issue() = UserRequestIssueEntity(
        githubIssueNumber = 1,
        groupId = "com.example",
        artifactId = "lib",
        version = "1.0.0",
    )

    private fun report(issue: UserRequestIssueEntity, failedAttempts: Int) = UserRequestReportEntity(
        userRequestIssue = issue,
        groupId = "com.example",
        artifactId = "lib",
        version = "1.0.0",
        indexingStatus = UserRequestIndexingStatus.SUCCESS,
        failedAttempts = failedAttempts,
    )
}
