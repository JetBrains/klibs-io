package io.klibs.integration.github.model

import java.time.Instant

data class GitHubRepository(
    val nativeId: Long,

    val name: String,
    val createdAt: Instant,
    val description: String? = null,
    val defaultBranch: String,

    val owner: String,

    val homepage: String? = null,

    val hasGhPages: Boolean,
    val hasIssues: Boolean,
    val hasWiki: Boolean,

    val stars: Int,
    val openIssues: Int? = null,
    val lastActivity: Instant,
)
