package io.klibs.core.pckg.service

import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SuspiciousPackageCandidateCollectionService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
) {

    @Transactional
    fun refreshCandidates(): CandidateRefreshSummary = CandidateRefreshSummary(
        inserted = suspiciousPackageCandidateRepository.insertMissingCandidates(),
        deleted = suspiciousPackageCandidateRepository.deleteStaleCandidates(),
    )
}

data class CandidateRefreshSummary(
    val inserted: Int,
    val deleted: Int,
)
