package io.klibs.integration.github.model

/**
 * Per-author commit count over a time window, derived from the GitHub GraphQL commit history.
 *
 * [identity] is the author's GitHub login when available, falling back to the commit's email
 * (for unattributed commits where the user isn't linked to a GitHub account).
 */
data class GitHubCommitAuthor(
    val identity: String,
    val commits: Int,
)
