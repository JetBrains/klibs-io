package io.klibs.integration.github.model

// --- GraphQL response shape, Maps directly to COMMIT_AUTHORS_QUERY. ---
internal data class GqlCommitAuthorsResponse(
    val data: GqlCommitsData? = null,
    val errors: List<com.fasterxml.jackson.databind.JsonNode>? = null,
)

internal data class GqlCommitsData(val repository: GqlRepository?)
internal data class GqlRepository(val defaultBranchRef: GqlBranchRef?)
internal data class GqlBranchRef(val target: GqlTarget?)
internal data class GqlTarget(val history: GqlHistory?)
internal data class GqlHistory(
    val nodes: List<GqlCommitNode> = emptyList(),
    val pageInfo: GqlPageInfo,
)
internal data class GqlCommitNode(val author: GqlAuthor?)
internal data class GqlAuthor(val user: GqlUser?, val email: String?)
internal data class GqlUser(val login: String?)
internal data class GqlPageInfo(val hasNextPage: Boolean, val endCursor: String?)
