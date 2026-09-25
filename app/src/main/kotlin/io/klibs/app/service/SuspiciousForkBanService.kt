package io.klibs.app.service

import io.klibs.core.pckg.dto.projection.ForkBanCandidateView
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import io.klibs.core.project.blacklist.CandidateBanRefusedException
import io.klibs.core.project.blacklist.SuspiciousPackageCandidateBanService
import io.klibs.integration.github.GitHubIntegration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SuspiciousForkBanService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val suspiciousPackageCandidateBanService: SuspiciousPackageCandidateBanService,
    private val gitHubIntegration: GitHubIntegration,
) {

    fun banConfirmedForks(): ForkBanSummary {
        val candidates = suspiciousPackageCandidateRepository.findForkBanCandidates()
        val checks = mutableMapOf<Pair<String, String>, ForkCheck>()

        val checked = candidates.map { candidate ->
            val parentFullName = "${candidate.repoOwner}/${candidate.repoName}"
            val check = checks.getOrPut(candidate.suspectOwner.lowercase() to parentFullName.lowercase()) {
                checkIsForkOf(candidate.suspectOwner, candidate.repoName, parentFullName)
            }
            candidate to check
        }

        val banned = checked.filter { it.second == ForkCheck.FORK }.count { ban(it.first) }

        return ForkBanSummary(
            evaluated = candidates.size,
            banned = banned,
            notFork = checked.count { it.second == ForkCheck.NOT_FORK },
            noDecision = checked.count { it.second == ForkCheck.NO_DECISION },
        )
    }

    private fun checkIsForkOf(suspectOwner: String, repoName: String, parentFullName: String): ForkCheck {
        val forkParentFullName = try {
            gitHubIntegration.getForkParentFullName(suspectOwner, repoName)
        } catch (e: Exception) {
            logger.warn("Could not look up GitHub repository {}/{}", suspectOwner, repoName, e)
            return ForkCheck.NO_DECISION
        }

        val isFork = forkParentFullName.equals(parentFullName, ignoreCase = true)
        return if (isFork) ForkCheck.FORK else ForkCheck.NOT_FORK
    }

    private fun ban(candidate: ForkBanCandidateView): Boolean {
        val forkFullName = "${candidate.suspectOwner}/${candidate.repoName}"
        val reason = "Auto-banned: $forkFullName is a fork of ${candidate.repoOwner}/${candidate.repoName}"
        try {
            suspiciousPackageCandidateBanService.banCandidate(
                projectId = candidate.projectId,
                artifactId = candidate.artifactId,
                groupId = candidate.groupId,
                reason = reason,
            )
        } catch (e: CandidateBanRefusedException) {
            logger.error("Could not ban {}:{}", candidate.groupId, candidate.artifactId, e)
            return false
        }

        logger.info("Banned {}:{}: {}", candidate.groupId, candidate.artifactId, reason)
        return true
    }

    private enum class ForkCheck { FORK, NOT_FORK, NO_DECISION }

    private companion object {
        private val logger = LoggerFactory.getLogger(SuspiciousForkBanService::class.java)
    }
}

data class ForkBanSummary(
    val evaluated: Int,
    val banned: Int,
    val notFork: Int,
    val noDecision: Int,
)
