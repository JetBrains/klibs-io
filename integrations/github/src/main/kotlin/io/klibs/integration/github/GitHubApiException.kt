package io.klibs.integration.github

/**
 * A GitHub response that is neither a success nor a 404, so it says nothing about whether the
 * requested resource exists. Carries the status so callers can tell a rate limit apart from a
 * rejected credential.
 */
class GitHubApiException(
    val status: Int,
    url: String,
    body: String,
) : RuntimeException("GitHub returned $status for $url: ${body.take(MAX_BODY_LENGTH)}")

private const val MAX_BODY_LENGTH = 300
