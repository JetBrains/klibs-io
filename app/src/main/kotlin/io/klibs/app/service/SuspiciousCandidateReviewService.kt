package io.klibs.app.service

import io.klibs.app.api.KeepSuspiciousCandidateRequest
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

@Service
class SuspiciousCandidateReviewService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val blacklistService: BlacklistService,
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

    /** Returns false when the groupId has no PENDING rows in the project. */
    fun keep(request: KeepSuspiciousCandidateRequest): Boolean =
        suspiciousPackageCandidateRepository.markGroupResolved(request.projectId, request.groupId, request.note) > 0

    @Transactional
    fun banCandidate(projectId: Int, artifactId: String, groupId: String, reason: String) {
        val resolved = suspiciousPackageCandidateRepository.markResolved(projectId, artifactId, groupId, reason)
        if (resolved == 0 || !blacklistService.banPackage(groupId, artifactId, reason)) {
            throw CandidateBanRefusedException(groupId, artifactId)
        }
    }

    private fun pendingCountOrNaN(): Double =
        runCatching { suspiciousPackageCandidateRepository.countByStatus(CandidateStatus.PENDING).toDouble() }
            .onFailure { logger.debug("Could not count pending suspicious package candidates", it) }
            .getOrDefault(Double.NaN)

    companion object {
        private val logger = LoggerFactory.getLogger(SuspiciousCandidateReviewService::class.java)

        const val METRIC_SUSPICIOUS_PACKAGE_CANDIDATE_PENDING = "klibs.suspicious.package.candidate.pending"
    }
}
