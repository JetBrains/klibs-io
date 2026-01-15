package io.klibs.core.search

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.model.TargetGroup
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class PackageSearchRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : PackageSearchRepository {

    /**
     * This implementation uses the package_index materialized view for efficient searching.
     *
     * WATCH OUT for SQL injection. Pass raw user input only as SQL params, no in-place usage.
     */
    override fun find(
        rawQuery: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        page: Int,
        limit: Int
    ): List<SearchPackageResult> {
        val isQueryPresent = rawQuery?.isBlank() == false
        val offset = limit * (page - 1)

        val exactMatchQuery = rawQuery?.normalizeSearchQuery()
        val exactMatchWildcardQuery = exactMatchQuery?.addWildcardPostfix()
        val (wildcardQueryWithSpecialSymbols, wildcardQueryWithoutSpecialSymbols) = rawQuery?.let {
            createWildcardSubqueries(
                it
            )
        } ?: Pair("", "")

        val targetCondition = formTargetCondition(targetFilters)

        val sql = buildString {
            append("SELECT group_id, artifact_id, latest_version, latest_description, release_ts, ")
            append("owner_type, owner_login, latest_license_name, array_to_string(platforms, ',') AS platforms, ")
            append("array_to_string(targets, ',') AS targets")

            // For debugging and testing purposes
            append(", targets_vector")

            if (isQueryPresent && sortBy == SearchSort.RELEVANCY) {
                append(
                    ", (ts_rank_cd(fts, :exactMatchQuery ::tsquery) * 0.7 + " +
                            "ts_rank_cd(fts, :wildcardQueryWithSpecialSymbols ::tsquery || wildcardQueryWithoutSpecialSymbols ::tsquery || :exactMatchWildcardQuery ::tsquery) * 0.3) AS weighted_rank"
                )
            }
            appendLine(" FROM package_index")

            var prefix = "WHERE"

            if (isQueryPresent) {
                appendLine(", to_tsquery('english', :wildcardQueryWithoutSpecialSymbols) wildcardQueryWithoutSpecialSymbols")
                appendLine(
                    " WHERE (:wildcardQueryWithSpecialSymbols ::tsquery || wildcardQueryWithoutSpecialSymbols || :exactMatchWildcardQuery ::tsquery) @@ fts"
                )
                prefix = "AND"
            }

            if (platforms.isNotEmpty()) {
                val platformsQuery = platforms.distinct().joinToString(separator = " & ") { it.name }
                appendLine(" $prefix platforms_vector @@ '$platformsQuery'")

                prefix = "AND"
            }

            if (ownerLogin != null) {
                appendLine(" $prefix owner_login = :ownerLogin")

                prefix = "AND"
            }

            if (targetCondition != null) {
                appendLine(" $prefix targets_vector @@ $targetCondition")

                prefix = "AND"
            }

            if (targetFilters.containsKey(TargetGroup.JavaScript)) {
                appendLine(" $prefix platforms_vector @@ 'JS'")

                prefix = "AND"
            }

            if (targetFilters.containsKey(TargetGroup.Wasm)) {
                appendLine(" $prefix platforms_vector @@ 'WASM'")
            }

            val orderBy = when {
                sortBy == SearchSort.RELEVANCY && isQueryPresent -> "weighted_rank DESC"
                else -> "release_ts DESC"
            }
            appendLine(" ORDER BY $orderBy")
            appendLine(" LIMIT $limit")
            appendLine(" OFFSET $offset")
        }

        @Suppress("SqlSourceToSinkFlow")
        val query = jdbcClient.sql(sql)
            .param("limit", limit)
            .param("offset", offset)
            .param("exactMatchQuery", exactMatchQuery)
            .param("exactMatchWildcardQuery", exactMatchWildcardQuery)
            .param("wildcardQueryWithSpecialSymbols", wildcardQueryWithSpecialSymbols)
            .param("wildcardQueryWithoutSpecialSymbols", wildcardQueryWithoutSpecialSymbols)
            .param("ownerLogin", ownerLogin)

        return query.query(PACKAGE_OVERVIEW_ROW_MAPPER).list()
    }

    /**
     * Processes a raw search query by splitting it into words, handling special characters, and appending
     * search-specific wildcard syntax for use in a full-text search. The method returns a Pair of strings
     * where:
     * - First component (withSpecialChars): Contains terms with special characters (punctuation), each wrapped in
     *   single quotes and with a wildcard suffix. Terms with apostrophes have them escaped by doubling.
     *   If no special characters are found, returns an empty string.
     * - Second component (withoutSpecialChars): Contains individual words without special characters and
     *   split words from the special character terms, each with a wildcard suffix.
     */
    internal fun createWildcardSubqueries(rawQuery: String): Pair<String, String> {
        assert(rawQuery.isNotBlank())

        val words = rawQuery.trim().split("\\s+".toRegex())

        val withSpecialChars =
            words.filter { it.hasSpecialCharacters() }.distinct()

        val withoutSpecialChars = (words.filter { !it.hasSpecialCharacters() } +
                withSpecialChars
                    .flatMap { it.split(Regex("\\p{Punct}")) }
                    .filter { it.isNotBlank() }).distinct()


        return Pair(withSpecialChars.map { it.normalizeSearchQuery() }.joinToQuery(), withoutSpecialChars.joinToQuery())
    }

    private fun List<String>.joinToQuery(): String = this.joinToString(separator = "|") { it.addWildcardPostfix() }

    private fun String.hasSpecialCharacters(): Boolean = contains(Regex("\\p{Punct}"))

    private fun String.normalizeSearchQuery(): String = escapeApostrophe().coverWithApostrophe()

    private fun String.coverWithApostrophe(): String = "'$this'"

    private fun String.escapeApostrophe(): String = replace("'", "''")

    private fun String.addWildcardPostfix(): String = "$this:*"

    override fun refreshIndex() {
        jdbcClient.sql("REFRESH MATERIALIZED VIEW CONCURRENTLY package_index")
            .update()
    }

    private companion object {
        private val PACKAGE_OVERVIEW_ROW_MAPPER = RowMapper<SearchPackageResult> { rs, _ ->
            val packageTargets = rs.getString("targets")
                .takeIf { it.isNotBlank() }
                ?.split(",")
                ?.map {
                    val parts = it.split("_", limit = 2)
                    val platform = PackagePlatform.valueOf (parts[0])

                    PackageTarget(
                        platform = platform,
                        target = parts.getOrNull(1)
                    )
                } ?: emptyList()

            SearchPackageResult(
                groupId = rs.getString("group_id"),
                artifactId = rs.getString("artifact_id"),
                description = rs.getString("latest_description"),
                ownerType = ScmOwnerType.findBySerializableName(rs.getString("owner_type")),
                ownerLogin = rs.getString("owner_login"),
                licenseName = rs.getString("latest_license_name"),
                latestVersion = rs.getString("latest_version"),
                releaseTs = rs.getTimestamp("release_ts").toInstant(),
                platforms = rs.getString("platforms")
                    .split(",")
                    .map { PackagePlatform.valueOf(it) },
                targetsList = packageTargets,
                targetsMap = packageTargets.filter {
                        it.target != null
                    }
                    .groupBy(
                        keySelector = { it: PackageTarget ->
                            TargetGroup.fromPlatformAndTarget(it.platform.name, it.target!!)
                        },
                        valueTransform = { it: PackageTarget ->
                            it.target!!
                        }
                    )
                    .mapValues { it.value.toSet() }
            )
        }
    }
}
