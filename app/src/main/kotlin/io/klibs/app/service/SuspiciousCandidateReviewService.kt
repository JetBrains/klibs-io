package io.klibs.app.service

import io.klibs.app.api.KeepSuspiciousCandidateRequest
import io.klibs.app.api.SuspiciousCandidateResponse
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SuspiciousCandidateReviewService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val blacklistService: BlacklistService,
) {

    fun findPending(): List<SuspiciousCandidateResponse> =
        suspiciousPackageCandidateRepository.findAllByStatus(CandidateStatus.PENDING)
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
}
