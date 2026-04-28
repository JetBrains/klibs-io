package io.klibs.integration.github.model

/**
 * Response of `GET /repos/{owner}/{repo}/stats/participation`:
 * 52 weekly commit counts (oldest first), where index 51 is the most recent full week.
 * [null] indicates GitHub returned `202 Accepted` (stats are being computed); caller should retry later.
 */
data class GitHubParticipationStats(
    val weeklyAllCommits: List<Int>,
)
