package io.klibs.core.search.repository

import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.opensearch.OpenSearchProperties
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
class PackageSearchRepositoryOpenSearch(
    client: OpenSearchClient,
    properties: OpenSearchProperties,
) : AbstractOpenSearchSearchRepository<SearchPackageResult>(client), PackageSearchRepository {

    override val indexName: String = properties.packageIndex

    override val sourceFields: List<String> = SOURCE_FIELDS

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
            add(match("group_id", query, 4))
            add(match("artifact_id", query, 4))
            add(match("owner_login", query, 4))
            add(match("latest_description", query, 2))
            add(fuzzy("group_id", query, 2))
            add(fuzzy("artifact_id", query, 2))
            add(phrasePrefix("group_id", query, 2))
            add(phrasePrefix("artifact_id", query, 2))
            if (multiWord) add(phrase("artifact_id", query, 4))
        }
    }

    override fun sortOptions(sortBy: SearchSort, isQueryPresent: Boolean): List<SortOptions> {
        val primary = if (sortBy == SearchSort.RELEVANCY && isQueryPresent) {
            scoreDesc()
        } else {
            fieldSort("release_ts", SortOrder.Desc)
        }
        return listOf(primary, fieldSort("group_id.keyword", SortOrder.Asc), fieldSort("artifact_id.keyword", SortOrder.Asc))
    }

    override fun toResult(src: ObjectNode): SearchPackageResult {
        val packageTargets = src.stringList("targets").map { t ->
            PackageTarget(PackagePlatform.valueOf(t.substringBefore('_')), t.substringAfter('_', "").ifEmpty { null })
        }
        return SearchPackageResult(
            groupId = src.get("group_id").asText(),
            artifactId = src.get("artifact_id").asText(),
            description = src.textOrNull("latest_description"),
            ownerType = ScmOwnerType.findBySerializableName(src.get("owner_type").asText()),
            ownerLogin = src.get("owner_login").asText(),
            licenseName = src.textOrNull("latest_license_name"),
            latestVersion = src.get("latest_version").asText(),
            releaseTs = LocalDateTime.parse(src.get("release_ts").asText()).toInstant(ZoneOffset.UTC),
            platforms = src.stringList("platforms").map { PackagePlatform.valueOf(it) },
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
        private val SOURCE_FIELDS = listOf(
            "group_id", "artifact_id", "project_id", "latest_version", "latest_description", "release_ts",
            "owner_type", "owner_login", "latest_license_name", "platforms", "targets",
        )
    }
}
