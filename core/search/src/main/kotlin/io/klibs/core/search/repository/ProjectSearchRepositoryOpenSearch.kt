package io.klibs.core.search.repository

import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.configuration.properties.OpenSearchProperties
import io.klibs.core.search.opensearch.OpenSearchQueryBuilder
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Primary
@Repository
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class ProjectSearchRepositoryOpenSearch(
    client: OpenSearchClient,
    properties: OpenSearchProperties,
) : AbstractOpenSearchSearchRepository<SearchProjectResult>(client), ProjectSearchRepository {

    override val indexName: String = properties.projectIndex

    override val excludedSourceFields: List<String> = EXCLUDED_SOURCE_FIELDS

    override fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int,
    ): List<SearchProjectResult> = doFind(
        query = query,
        platforms = platforms,
        targetFilters = targetFilters,
        ownerLogin = ownerLogin,
        sortBy = sortBy,
        page = page,
        limit = limit,
        extraFilters = extraFilters(tags, markers),
        popularityScored = true,
    )

    override fun shouldClauses(query: String): List<Query> = buildList {
        val multiWord = query.contains(' ')
        with(OpenSearchQueryBuilder) {
            add(match("owner_login", query, 4))
            add(match("name", query, 4))
            add(match("group_ids", query, 4))
            add(match("artifact_ids", query, 4))
            add(match("tags", query, 3))
            add(match("project_description", query, 2))
            add(match("repo_description", query, 2))
            add(fuzzy("name", query, 2))
            add(fuzzy("artifact_ids", query, 2))
            add(phrasePrefix("name", query, 3))
            add(phrasePrefix("owner_login", query, 3))
            add(phrasePrefix("artifact_ids", query, 2))
            add(phrasePrefix("group_ids", query, 2))
            if (multiWord) {
                add(phrase("name", query, 6))
                add(phrase("artifact_ids", query, 4))
            }
        }
    }

    private fun extraFilters(tags: List<String>, markers: List<String>): List<Query> = buildList {
        tags.forEach { add(OpenSearchQueryBuilder.term("tags.keyword", it)) }
        if (markers.isNotEmpty()) add(OpenSearchQueryBuilder.termsAny("markers", markers))
    }

    override fun sortOptions(sortBy: SearchSort, isQueryPresent: Boolean): List<SortOptions> {
        val primary = when (sortBy) {
            // OSS health is not indexed in OpenSearch, so this sort cannot be served here.
            SearchSort.MOST_HEALTHY -> throw UnsupportedOperationException("$sortBy is not supported by OpenSearch")
            SearchSort.RELEVANCY if isQueryPresent -> scoreDesc()
            SearchSort.MOST_DEPENDENTS -> fieldSort("dependent_count", SortOrder.Desc)
            else -> fieldSort("stars", SortOrder.Desc)
        }
        return listOf(primary, fieldSort("project_id", SortOrder.Asc))
    }

    override fun toResult(src: ObjectNode): SearchProjectResult = SearchProjectResult(
        id = src.get("project_id").asInt(),
        name = src.get("name").asText(),
        repoName = src.get("repo_name").asText(),
        description = src.textOrNull("plain_description"),
        vcsStars = src.get("stars").asInt(),
        ownerType = ScmOwnerType.findBySerializableName(src.get("owner_type").asText()),
        ownerLogin = src.get("owner_login").asText(),
        licenseName = src.textOrNull("license_name"),
        latestVersion = src.get("latest_version").asText(),
        latestVersionPublishedAt = LocalDateTime.parse(src.get("latest_version_ts").asText()).toInstant(ZoneOffset.UTC),
        platforms = src.stringList("platforms").map { PackagePlatform.valueOf(it) },
        targets = src.stringList("targets"),
        tags = src.stringList("tags"),
        markers = src.stringList("markers"),
        dependentCount = src.get("dependent_count").asInt(),
        // Not indexed in OpenSearch, see sortOptions.
        ossHealthScore = null,
    )

    private companion object {
        private val EXCLUDED_SOURCE_FIELDS = listOf(
            "packages", "project_description", "repo_description", "group_ids", "artifact_ids", "has_readme",
        )
    }
}
