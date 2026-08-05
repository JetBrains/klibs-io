package io.klibs.core.search.repository

import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.opensearch.IndexNaming
import io.klibs.core.search.opensearch.OpenSearchQueryBuilder
import io.klibs.core.search.opensearch.PackageFields
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
class PackageSearchRepositoryOpenSearch(
    client: OpenSearchClient,
    naming: IndexNaming,
) : AbstractOpenSearchSearchRepository<SearchPackageResult>(client), PackageSearchRepository {

    override val indexName: String = naming.packages.alias

    override val excludedSourceFields: List<String> = EXCLUDED_SOURCE_FIELDS

    override fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        page: Int,
        limit: Int,
    ): List<SearchPackageResult> = doFind(
        query = query,
        platforms = platforms,
        targetFilters = targetFilters,
        ownerLogin = ownerLogin,
        sortBy = sortBy,
        page = page,
        limit = limit,
    )

    override fun shouldClauses(query: String): List<Query> = buildList {
        val multiWord = query.contains(' ')
        with(OpenSearchQueryBuilder) {
            add(match(PackageFields.GROUP_ID, query, 4))
            add(match(PackageFields.ARTIFACT_ID, query, 4))
            add(match(PackageFields.OWNER_LOGIN, query, 4))
            add(match(PackageFields.LATEST_DESCRIPTION, query, 2))
            add(fuzzy(PackageFields.GROUP_ID, query, 2))
            add(fuzzy(PackageFields.ARTIFACT_ID, query, 2))
            add(phrasePrefix(PackageFields.GROUP_ID, query, 2))
            add(phrasePrefix(PackageFields.ARTIFACT_ID, query, 2))
            if (multiWord) add(phrase(PackageFields.ARTIFACT_ID, query, 4))
        }
    }

    override fun sortOptions(sortBy: SearchSort, isQueryPresent: Boolean): List<SortOptions> {
        val primary = if (sortBy == SearchSort.RELEVANCY && isQueryPresent) {
            scoreDesc()
        } else {
            fieldSort(PackageFields.RELEASE_TS, SortOrder.Desc)
        }
        // group_id + artifact_id are there to make sorting stable
        return listOf(
            primary,
            fieldSort(PackageFields.GROUP_ID.keyword, SortOrder.Asc),
            fieldSort(PackageFields.ARTIFACT_ID.keyword, SortOrder.Asc),
        )
    }

    override fun toResult(src: ObjectNode): SearchPackageResult {
        val packageTargets = src.stringList(PackageFields.TARGETS).map { t ->
            PackageTarget(PackagePlatform.valueOf(t.substringBefore('_')), t.substringAfter('_', "").ifEmpty { null })
        }
        return SearchPackageResult(
            groupId = src.get(PackageFields.GROUP_ID).asText(),
            artifactId = src.get(PackageFields.ARTIFACT_ID).asText(),
            description = src.textOrNull(PackageFields.LATEST_DESCRIPTION),
            ownerType = ScmOwnerType.findBySerializableName(src.get(PackageFields.OWNER_TYPE).asText()),
            ownerLogin = src.get(PackageFields.OWNER_LOGIN).asText(),
            licenseName = src.textOrNull(PackageFields.LATEST_LICENSE_NAME),
            latestVersion = src.get(PackageFields.LATEST_VERSION).asText(),
            releaseTs = LocalDateTime.parse(src.get(PackageFields.RELEASE_TS).asText()).toInstant(ZoneOffset.UTC),
            platforms = src.stringList(PackageFields.PLATFORMS).map { PackagePlatform.valueOf(it) },
            targetsList = packageTargets,
            targetsMap = packageTargets.filter { it.target != null }
                .groupBy(
                    keySelector = { TargetGroup.fromPlatformAndTarget(it.platform.name, it.target!!) },
                    valueTransform = { it.target!! },
                )
                .mapValues { it.value.toSet() },
        )
    }

    private companion object {
        private val EXCLUDED_SOURCE_FIELDS = listOf(PackageFields.PROJECT_ID, PackageFields.LATEST_PACKAGE_ID)
    }
}
