package io.klibs.app.service

import io.klibs.app.api.ReviewSuspiciousCandidateDecision
import io.klibs.app.api.ReviewSuspiciousCandidatesRequest
import io.klibs.app.api.SuspiciousCandidateDecision
import io.klibs.app.api.SuspiciousCandidateReviewOutcome
import io.klibs.app.api.SuspiciousCandidateReviewResult
import io.klibs.app.api.SuspiciousCandidateResponse
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class SuspiciousCandidateReviewService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val blacklistService: BlacklistService,
    private val transactionTemplate: TransactionTemplate,
    registry: MeterRegistry,
) {

    init {
        Gauge.builder(METRIC_SUSPICIOUS_PACKAGE_CANDIDATE_PENDING) { pendingCountOrNaN() }
            .description("Klibs: Number of suspicious package candidates with status PENDING")
            .register(registry)
    }

    fun findPending(page: Int, limit: Int): List<SuspiciousCandidateResponse> =
        suspiciousPackageCandidateRepository.findAllByStatus(
            CandidateStatus.PENDING,
            PageRequest.of(page - 1, limit, Sort.by("projectId", "groupId", "artifactId")),
        )
            .map { SuspiciousCandidateResponse(it.projectId, it.groupId, it.artifactId, it.detectedAt) }

    fun review(request: ReviewSuspiciousCandidatesRequest): List<SuspiciousCandidateReviewResult> {
        val decisions = request.decisions
        require(decisions.map { it.projectId to it.groupId }.distinct().size == decisions.size) {
            "Only one decision per projectId and groupId is allowed"
        }

        val keeps = decisions.filter { it.decision == SuspiciousCandidateDecision.KEEP }
            .map(::keep)
        val bans = decisions.filter { it.decision == SuspiciousCandidateDecision.BAN }
            .flatMap(::ban)
        return keeps + bans
    }

    private fun keepReviewed(
        projectId: Int,
        groupId: String,
        artifacts: List<String>,
        note: String,
    ): SuspiciousCandidateReviewResult {
        val pending = suspiciousPackageCandidateRepository.findAllByProjectIdAndGroupIdAndStatus(
            projectId, groupId, CandidateStatus.PENDING
        )
        if (pending.isEmpty()) {
            return SuspiciousCandidateReviewResult(
                projectId, groupId, null, SuspiciousCandidateDecision.KEEP, SuspiciousCandidateReviewOutcome.SKIP,
                "No PENDING rows for this groupId in the project",
            )
        }

        val newArtifacts = pending.map { it.artifactId }.toSet() - artifacts.toSet()
        if (newArtifacts.isNotEmpty()) {
            return SuspiciousCandidateReviewResult(
                projectId, groupId, null, SuspiciousCandidateDecision.KEEP, SuspiciousCandidateReviewOutcome.SKIP,
                "New PENDING artifacts since the review: ${newArtifacts.sorted()}",
            )
        }

        pending.forEach { candidate ->
            check(
                suspiciousPackageCandidateRepository.markResolved(
                    projectId, candidate.artifactId, groupId, note
                ) == 1
            ) { "PENDING row changed while reviewing ${candidate.artifactId}" }
        }
        return SuspiciousCandidateReviewResult(
            projectId, groupId, null, SuspiciousCandidateDecision.KEEP, SuspiciousCandidateReviewOutcome.OK,
            "Resolved ${pending.size} PENDING row(s)",
        )
    }

    private fun pendingArtifactIds(projectId: Int, groupId: String): Set<String> =
        suspiciousPackageCandidateRepository.findAllByProjectIdAndGroupIdAndStatus(
            projectId, groupId, CandidateStatus.PENDING
        ).map { it.artifactId }.toSet()

    @Transactional
    fun banCandidate(projectId: Int, artifactId: String, groupId: String, reason: String) {
        val resolved = suspiciousPackageCandidateRepository.markResolved(projectId, artifactId, groupId, reason)
        if (resolved == 0 || !blacklistService.banPackage(groupId, artifactId, reason)) {
            throw CandidateBanRefusedException(groupId, artifactId)
        }
    }

    private fun keep(decision: ReviewSuspiciousCandidateDecision): SuspiciousCandidateReviewResult = try {
        transactionTemplate.execute {
            keepReviewed(decision.projectId, decision.groupId, decision.artifacts, decision.note)
        }
    } catch (e: Exception) {
        logger.error("Could not apply suspicious-candidate KEEP for ${decision.groupId}", e)
        result(decision, null, SuspiciousCandidateReviewOutcome.FAIL, e.message ?: "Unexpected error")
    }

    private fun ban(decision: ReviewSuspiciousCandidateDecision): List<SuspiciousCandidateReviewResult> {
        val results = decision.artifacts.map { artifactId ->
            try {
                transactionTemplate.execute {
                    banCandidate(decision.projectId, artifactId, decision.groupId, decision.note)
                }
                result(decision, artifactId, SuspiciousCandidateReviewOutcome.OK, "Resolved and banned")
            } catch (e: CandidateBanRefusedException) {
                result(decision, artifactId, SuspiciousCandidateReviewOutcome.SKIP, e.message ?: "Ban refused")
            } catch (e: Exception) {
                logger.error("Could not apply suspicious-candidate BAN for ${decision.groupId}:$artifactId", e)
                result(decision, artifactId, SuspiciousCandidateReviewOutcome.FAIL, e.message ?: "Unexpected error")
            }
        }.toMutableList()

        val newPending = pendingArtifactIds(decision.projectId, decision.groupId) - decision.artifacts.toSet()
        newPending.sorted().forEach { artifactId ->
            results += result(
                decision, artifactId, SuspiciousCandidateReviewOutcome.SKIP,
                "New PENDING artifact since the review; it stays PENDING",
            )
        }
        return results
    }

    private fun result(
        decision: ReviewSuspiciousCandidateDecision,
        artifactId: String?,
        outcome: SuspiciousCandidateReviewOutcome,
        message: String,
    ) = SuspiciousCandidateReviewResult(
        decision.projectId, decision.groupId, artifactId, decision.decision, outcome, message
    )

    private fun pendingCountOrNaN(): Double =
        runCatching { suspiciousPackageCandidateRepository.countByStatus(CandidateStatus.PENDING).toDouble() }
            .onFailure { logger.debug("Could not count pending suspicious package candidates", it) }
            .getOrDefault(Double.NaN)

    companion object {
        private val logger = LoggerFactory.getLogger(SuspiciousCandidateReviewService::class.java)

        const val METRIC_SUSPICIOUS_PACKAGE_CANDIDATE_PENDING = "klibs.suspicious.package.candidate.pending"
    }
}
