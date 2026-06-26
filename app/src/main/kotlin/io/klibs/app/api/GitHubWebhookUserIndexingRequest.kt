package io.klibs.app.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Request DTO for GitHub `issues` connecting with indexing requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubWebhookUserIndexingRequest(
    val action: String? = null,
    val issue: IssueDto? = null
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class IssueDto(
        val number: Int? = null,
        val body: String? = null,
        val labels: List<LabelDto> = emptyList(),
        @field:JsonProperty("created_at")
        val createdAt: Instant? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LabelDto(
        val name: String
    )
}