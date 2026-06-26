package io.klibs.core.pckg.dto

import java.time.Instant

/**
 * Service DTO for processing user library indexing requests.
 */
data class UserIndexingRequestDto(
    val githubIssueNumber: Int,
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
    val createdAt: Instant
)
