package io.klibs.core.search

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class ProjectSearchRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ProjectSearchRepository {

    override fun findRandomByStars(minStars: Int, maxStars: Int, limit: Int): List<SearchProjectResult> {
        val sql = """
            SELECT project_id,
                   owner_type,
                   owner_login,
                   repo_name,
                   name,
                   stars,
                   license_name,
                   latest_version,
                   latest_version_ts,
                   array_to_string(platforms, ',') AS platforms,
                   plain_description,
                   tags
            FROM project_index
            WHERE stars BETWEEN :minStars AND :maxStars
            ORDER BY random()
            LIMIT :limit
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("minStars", minStars)
            .param("maxStars", maxStars)
            .param("limit", limit)
            .query(PROJECT_OVERVIEW_ROW_MAPPER)
            .list()
    }

    override fun findRecentlyCreatedWithGoodQuality(searchLimit: Int, resultLimit: Int): List<SearchProjectResult> {
        val sql = """
            WITH matching_project_ids AS (SELECT project.id
                                          FROM scm_repo
                                                   JOIN project ON project.scm_repo_id = scm_repo.id
                                          WHERE scm_repo.has_readme = true
                                            AND scm_repo.license_key IS NOT NULL
                                            AND scm_repo.description IS NOT NULL
                                          ORDER BY scm_repo.created_ts DESC
                                          LIMIT :searchLimit)
            SELECT project_id,
                   owner_type,
                   owner_login,
                   repo_name,
                   name,
                   stars,
                   license_name,
                   latest_version,
                   latest_version_ts,
                   array_to_string(platforms, ',') AS platforms,
                   plain_description,
                   tags
            FROM project_index
            WHERE project_id IN (SELECT id FROM matching_project_ids)
            ORDER BY random()
            LIMIT :resultLimit
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("searchLimit", searchLimit)
            .param("resultLimit", resultLimit)
            .query(PROJECT_OVERVIEW_ROW_MAPPER)
            .list()
    }

    /**
     * This is an ugly implementation with StringBuilder, but FTS needs to be reimplemented anyway.
     *
     * WATCH OUT for SQL injection. Pass raw user input only as SQL params, no in-place usage.
     */
    override fun find(
        rawQuery: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int
    ): List<SearchProjectResult> {
        val isQueryPresent = rawQuery?.isBlank() == false
        val offset = limit * (page - 1)
        val orderBy = when {
            sortBy == SearchSort.RELEVANCY && isQueryPresent -> "weighted_rank DESC"
            sortBy == SearchSort.MOST_STARS -> "stars DESC"
            else -> "stars DESC"
        }

        val exactMatchQuery = rawQuery?.normalizeSearchQuery()
        val exactMatchWildcardQuery = rawQuery?.normalizeSearchQuery()?.addWildcardPostfix()
        val (wildcardQueryWithSpecialSymbols, wildcardQueryWithoutSpecialSymbols) = rawQuery?.let {
            createWildcardSubqueries(
                it
            )
        } ?: Pair("", "")

        val targetCondition = formTargetCondition(targetFilters)

        val sql = buildString {
            append("SELECT project_id, owner_type, owner_login, repo_name, name, stars, license_name, latest_version")
            append(", latest_version_ts, array_to_string(platforms, ',') AS platforms, plain_description, tags, markers")

            // For debugging and testing purposes
            append(", targets_vector")

            if (isQueryPresent && sortBy == SearchSort.RELEVANCY) {
                append(
                    ", (ts_rank_cd(fts, :exactMatchQuery ::tsquery) * 0.7 + " +
                            "ts_rank_cd(fts, :wildcardQueryWithSpecialSymbols ::tsquery || wildcardQueryWithoutSpecialSymbols || :exactMatchWildcardQuery ::tsquery) * 0.3 + " +
                            "log(stars + 1) * 0.7) AS weighted_rank"
                )
            }
            appendLine(" FROM project_index")

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

            if (tags.isNotEmpty()) {
                appendLine(" $prefix tags @> ARRAY[:tags]::text[]")

                prefix = "AND"
            }

            if (markers.isNotEmpty()) {
                appendLine(" $prefix markers && :markers::varchar[]")
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

            appendLine(" ORDER BY $orderBy")
            appendLine(" LIMIT $limit")
            appendLine(" OFFSET $offset")
        }

        @Suppress("SqlSourceToSinkFlow")
        return jdbcClient.sql(sql)
            .param("exactMatchQuery", exactMatchQuery)
            .param("exactMatchWildcardQuery", exactMatchWildcardQuery)
            .param("wildcardQueryWithSpecialSymbols", wildcardQueryWithSpecialSymbols)
            .param("wildcardQueryWithoutSpecialSymbols", wildcardQueryWithoutSpecialSymbols)
            .param("ownerLogin", ownerLogin)
            .param("tags", tags.toTypedArray())
            .param("markers", markers.toTypedArray())
            .query(PROJECT_OVERVIEW_ROW_MAPPER)
            .list()
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
     *
     * Example 1:
     * ```
     * Input: "api-server:1.0 stable"
     * Output: Pair(
     *   first = "'api-server:1.0':*",
     *   second = "api:*|server:*|1:*|0:*|stable:*"
     * )
     * ```
     *
     * Example 2:
     * ```
     * Input: "user's guide"
     * Output: Pair(
     *   first = "'user''s':*",
     *   second = "guide:*|user:*|s:*"
     * )
     * ```
     *
     * Example 3:
     * ```
     * Input: "kotlin java"
     * Output: Pair(
     *   first = "",
     *   second = "kotlin:*|java:*"
     * )
     * ```
     *
     * @param rawQuery The raw user-inputted search query string that may include spaces or special characters.
     * @return A Pair where first component contains terms with special characters and second component contains
     *         individual words, all formatted for full-text search with wildcards.
     * @throws AssertionError if the input query is blank
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
        jdbcClient.sql("REFRESH MATERIALIZED VIEW CONCURRENTLY project_index")
            .update()
    }

    private companion object {
        private val PROJECT_OVERVIEW_ROW_MAPPER = RowMapper<SearchProjectResult> { rs, _ ->
            SearchProjectResult(
                id = rs.getInt("project_id"),
                name = rs.getString("name"),
                repoName = rs.getString("repo_name"),
                description = rs.getString("plain_description"),
                vcsStars = rs.getInt("stars"),
                ownerType = ScmOwnerType.findBySerializableName(rs.getString("owner_type")),
                ownerLogin = rs.getString("owner_login"),
                licenseName = rs.getString("license_name"),
                latestVersion = rs.getString("latest_version"),
                latestVersionPublishedAt = rs.getTimestamp("latest_version_ts").toInstant(),
                platforms = rs.getString("platforms")
                    .split(",")
                    .map { PackagePlatform.valueOf(it) },
                markers = rs.getArray("markers")?.array?.let { it as? Array<*> }?.map { it.toString() } ?: emptyList(),
                targets = rs.getString("targets_vector").split(" ").map { it.trim('\'') },
                tags = rs.getArray("tags")?.array?.let { it as? Array<*> }?.map { it.toString() } ?: emptyList()
            )
        }
    }
}
