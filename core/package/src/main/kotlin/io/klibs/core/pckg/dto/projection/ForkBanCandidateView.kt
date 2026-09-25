package io.klibs.core.pckg.dto.projection

interface ForkBanCandidateView {
    val projectId: Int
    val artifactId: String
    val groupId: String
    val suspectOwner: String
    val repoOwner: String
    val repoName: String
}
