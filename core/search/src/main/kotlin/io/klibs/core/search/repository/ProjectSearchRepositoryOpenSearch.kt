package io.klibs.core.search.repository

import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.opensearch.OpenSearchQueryBuilder
import io.klibs.core.search.opensearch.ProjectFields
import io.klibs.core.search.opensearch.metrics.SearchQueryMetrics
import io.klibs.core.search.opensearch.keyword
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class ProjectSearchRepositoryOpenSearch(
    client: OpenSearchClient,
    projectIndexSpec: OpenSearchIndexSpec,
    metrics: SearchQueryMetrics,
) : AbstractOpenSearchSearchRepository<SearchProjectResult>(client, metrics), ProjectSearchRepository {

    override val spec: OpenSearchIndexSpec = projectIndexSpec

    override val excludedSourceFields: List<String> = EXCLUDED_SOURCE_FIELDS

    override fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int,
    ): List<SearchProjectResult> = doFind(
        query = query,
        platforms = platforms,
        targetGroupFilters = targetGroupFilters,
        ownerLogin = ownerLogin,
        sortBy = sortBy,
        page = page,
        limit = limit,
        extraFilters = extraFilters(tags, markers),
        popularityScored = true,
    )

    override fun shouldClauses(query: String): List<Query> = buildList {
        val multiWord = query.contains(' ')
        val partialLengths = OpenSearchQueryBuilder.MIN_PARTIAL_LENGTH..OpenSearchQueryBuilder.MAX_PARTIAL_LENGTH
        val shouldSplitToNgram = !multiWord && query.length in partialLengths

        with(OpenSearchQueryBuilder) {
            // Tier 1 — an exact term in an identity field, or a curated alias for one.
            add(match(ProjectFields.OWNER_LOGIN, query, 4))
            add(match(ProjectFields.NAME, query, 4))
            add(match(ProjectFields.GROUP_IDS, query, 4))
            add(match(ProjectFields.ARTIFACT_IDS, query, 4))
            add(toolAlias(ProjectFields.NAME, query, 8))
            add(toolAlias(ProjectFields.OWNER_LOGIN, query, 8))
            if (multiWord) {
                add(phrase(ProjectFields.NAME, query, 6))
                add(phrase(ProjectFields.ARTIFACT_IDS, query, 4))
            }

            // Tier 2 — the start of an identity field: a partly-typed query
            if (shouldSplitToNgram) {
                add(tokenPrefix(ProjectFields.NAME, query, PREFIX_BOOST))
                add(tokenPrefix(ProjectFields.ARTIFACT_IDS, query, PREFIX_BOOST))
            } else {
                // A one-term phrase is the same query as `match` above, so only a query too long or
                // too multi-word for the ngrams gains anything from the phrase-prefix form.
                add(phrasePrefix(ProjectFields.NAME, query, 3))
                add(phrasePrefix(ProjectFields.ARTIFACT_IDS, query, 2))
                add(phrasePrefix(ProjectFields.OWNER_LOGIN, query, 3))
                add(phrasePrefix(ProjectFields.GROUP_IDS, query, 2))
            }

            // Tier 3 — approximate readings of the same name, best-of rather than summed.
            add(bestOf(approximateNameClauses(query, shouldSplitToNgram), TIE_BREAKER))

            // Other fields
            add(match(ProjectFields.TAGS, query, 8))
            add(match(ProjectFields.PROJECT_DESCRIPTION, query, 5))
            add(match(ProjectFields.REPO_DESCRIPTION, query, 5))
            add(english(ProjectFields.TAGS, query, 8 * STEMMED_SHARE))
            add(english(ProjectFields.PROJECT_DESCRIPTION, query, 5 * STEMMED_SHARE))
            add(english(ProjectFields.REPO_DESCRIPTION, query, 5 * STEMMED_SHARE))
        }
    }

    private fun approximateNameClauses(query: String, shouldSplitToNgram: Boolean): List<Query> = buildList {
        with(OpenSearchQueryBuilder) {
            add(fuzzy(ProjectFields.NAME, query, 2))
            add(fuzzy(ProjectFields.ARTIFACT_IDS, query, 2))
            if (shouldSplitToNgram) {
                add(tokenSuffix(ProjectFields.NAME, query, SUFFIX_BOOST))
                add(tokenSuffix(ProjectFields.ARTIFACT_IDS, query, SUFFIX_BOOST))
            }
            // `ComposeCharts` is one token to every other analyzer, so the query `charts` cannot
            // reach it at all without the delimiter split.
            add(split(ProjectFields.NAME, query, 4f))
            add(english(ProjectFields.NAME, query, 2f))
        }
    }

    private fun extraFilters(tags: List<String>, markers: List<String>): List<Query> = buildList {
        tags.forEach { add(OpenSearchQueryBuilder.term(ProjectFields.TAGS.keyword, it)) }
        if (markers.isNotEmpty()) add(OpenSearchQueryBuilder.termsAny(ProjectFields.MARKERS, markers))
    }

    override fun sortOptions(sortBy: SearchSort, isQueryPresent: Boolean): List<SortOptions> {
        val primary = when (sortBy) {
            // OSS health is not indexed in OpenSearch, so this sort cannot be served here.
            SearchSort.MOST_HEALTHY -> throw UnsupportedOperationException("$sortBy is not supported by OpenSearch")
            SearchSort.RELEVANCY if isQueryPresent -> scoreDesc()
            SearchSort.MOST_DEPENDENTS -> fieldSort(ProjectFields.DEPENDENT_COUNT, SortOrder.Desc)
            else -> fieldSort(ProjectFields.STARS, SortOrder.Desc)
        }
        // project_id is there to make sorting stable
        return listOf(primary, fieldSort(ProjectFields.PROJECT_ID, SortOrder.Asc))
    }

    override fun toResult(src: ObjectNode): SearchProjectResult = SearchProjectResult(
        id = src.get(ProjectFields.PROJECT_ID).asInt(),
        name = src.get(ProjectFields.NAME).asText(),
        repoName = src.get(ProjectFields.REPO_NAME).asText(),
        description = src.textOrNull(ProjectFields.PLAIN_DESCRIPTION),
        vcsStars = src.get(ProjectFields.STARS).asInt(),
        ownerType = ScmOwnerType.findBySerializableName(src.get(ProjectFields.OWNER_TYPE).asText()),
        ownerLogin = src.get(ProjectFields.OWNER_LOGIN).asText(),
        licenseName = src.textOrNull(ProjectFields.LICENSE_NAME),
        latestVersion = src.get(ProjectFields.LATEST_VERSION).asText(),
        latestVersionPublishedAt = LocalDateTime.parse(src.get(ProjectFields.LATEST_VERSION_TS).asText())
            .toInstant(ZoneOffset.UTC),
        platforms = src.stringList(ProjectFields.PLATFORMS).map { PackagePlatform.valueOf(it) },
        targets = src.stringList(ProjectFields.TARGETS),
        tags = src.stringList(ProjectFields.TAGS),
        markers = src.stringList(ProjectFields.MARKERS),
        dependentCount = src.get(ProjectFields.DEPENDENT_COUNT).asInt(),
        // Not indexed in OpenSearch, see sortOptions.
        ossHealthScore = null,
    )

    private companion object {
        /** People type the start of a name far more often than its end. */
        private const val PREFIX_BOOST = 4.0f
        private const val SUFFIX_BOOST = 1.0f

        /** How much a losing alternative still contributes in tier 3. */
        private const val TIE_BREAKER = 0.2f

        /** A stemmed match is worth this share of the exact-form match on the same field. */
        private const val STEMMED_SHARE = 0.3f

        private val EXCLUDED_SOURCE_FIELDS = listOf(
            ProjectFields.PACKAGES,
            ProjectFields.PROJECT_DESCRIPTION,
            ProjectFields.REPO_DESCRIPTION,
            ProjectFields.GROUP_IDS,
            ProjectFields.ARTIFACT_IDS,
            ProjectFields.HAS_README,
        )
    }
}
