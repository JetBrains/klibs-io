package io.klibs.app.mapper

import io.klibs.app.api.GitHubWebhookUserIndexingRequest
import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.utils.UserIndexingRequestParser
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GitHubWebhookMapper(
    private val userIndexingRequestParser: UserIndexingRequestParser
) {

    /**
     * Maps GitHub issue DTO to service DTO, parsing the body for Maven coordinates.
     * Returns null if coordinates cannot be parsed or required fields are missing.
     */
    fun toUserRequestIssueDto(issue: GitHubWebhookUserIndexingRequest.IssueDto): UserIndexingRequestDto? {
        val number = issue.number ?: return null
        val parsed = userIndexingRequestParser.parseBody(issue.body) ?: return null
        return UserIndexingRequestDto(
            githubIssueNumber = number,
            groupId = parsed.groupId,
            artifactId = parsed.artifactId,
            version = parsed.version,
            createdAt = issue.createdAt ?: Instant.now()
        )
    }
}
