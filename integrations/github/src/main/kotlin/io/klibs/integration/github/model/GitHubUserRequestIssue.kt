package io.klibs.integration.github.model

import java.time.Instant

data class GitHubUserRequestIssue(
    val number: Int,
    val body: String?,
    val labels: List<String>,
    val createdAt: Instant,
)

data class GitHubUserRequestIssuesBatch(
    val issues: List<GitHubUserRequestIssue>,
    val hasMore: Boolean? = null
)