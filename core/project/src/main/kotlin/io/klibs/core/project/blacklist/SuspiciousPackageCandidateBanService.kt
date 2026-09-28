package io.klibs.core.project.blacklist

import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SuspiciousPackageCandidateBanService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val blacklistService: BlacklistService,
) {

    @Transactional
    fun banCandidate(projectId: Int, artifactId: String, groupId: String, reason: String) {
        val resolved = suspiciousPackageCandidateRepository.markResolved(projectId, artifactId, groupId, reason)
        if (resolved == 0 || !blacklistService.banPackage(groupId, artifactId, reason)) {
            throw CandidateBanRefusedException(groupId, artifactId)
        }
    }
}
