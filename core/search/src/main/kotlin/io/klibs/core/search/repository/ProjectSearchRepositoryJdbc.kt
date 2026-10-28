package io.klibs.core.search.repository

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.pckg.util.getTargetsVector
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.Category
import io.klibs.core.search.dto.repository.SearchProjectResult
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class ProjectSearchRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ProjectSearchRepository {

    /**
     * This is an ugly implementation with StringBuilder, but FTS needs to be reimplemented anyway.
     *
     * WATCH OUT for SQL injection. Pass raw user input only as SQL params, no in-place usage.
     */
    override fun find(
        rawQuery: String?,
        platforms: List<PackagePlatform>,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int,
        ownerLogins: List<String>?,
    ): List<SearchProjectResult> {
        val isQueryPresent = rawQuery?.isBlank() == false
        val offset = limit * (page - 1)
        val orderBy = when {
            sortBy == SearchSort.RELEVANCY && isQueryPresent -> "weighted_rank DESC, project_id ASC"
            sortBy == SearchSort.MOST_DEPENDENTS -> "dependent_count DESC, project_id ASC"
            sortBy == SearchSort.MOST_HEALTHY -> "health_score DESC NULLS LAST, project_id ASC"
            sortBy == SearchSort.MOST_STARS -> "stars DESC, project_id ASC"
            else -> "stars DESC, project_id ASC"
        }

        val exactMatchQuery = rawQuery?.normalizeSearchQuery()
        val exactMatchWildcardQuery = rawQuery?.normalizeSearchQuery()?.addWildcardPostfix()
        val (wildcardQueryWithSpecialSymbols, wildcardQueryWithoutSpecialSymbols) = rawQuery?.let {
            createWildcardSubqueries(
                it
            )
        } ?: Pair("", "")

        val targetCondition = formTargetCondition(targetGroupFilters)

        val sql = buildString {
            append("SELECT project_id, owner_type, owner_login, repo_name, name, stars, license_name, latest_version")
            append(", latest_version_ts, array_to_string(platforms, ',') AS platforms, plain_description, tags, markers, dependent_count, health_score")

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

            if (ownerLogins != null) {
                appendLine(" $prefix owner_login = ANY(:ownerLogins::text[])")

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
                appendLine(" $prefix $targetCondition")
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
            .param("ownerLogins", ownerLogins?.toTypedArray())
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

    fun findCategoriesWithProjects(
        limit: Int,
        ownerCategories: Map<String, List<String>> = emptyMap(),
    ): Map<Category, List<SearchProjectResult>> {
        val ownerCategoryEntries = ownerCategories.flatMap { (categoryName, ownerLogins) ->
            ownerLogins.map { ownerLogin -> categoryName to ownerLogin }
        }
        val ownerCategoryCte = ownerCategoryEntries.takeIf { it.isNotEmpty() }?.let { entries ->
            """
                WITH selector_owners(category_name, owner_login) AS (
                    VALUES ${entries.indices.joinToString(", ") { index ->
                        "(:selectorCategory$index::text, :selectorOwner$index::text)"
                    }}
                )
            """.trimIndent()
        }.orEmpty()
        val hasOwnerSelector = ownerCategoryEntries.isNotEmpty()
        val selectorExists = "EXISTS (SELECT 1 FROM selector_owners WHERE category_name = c.name)"
        val projectFilter = if (hasOwnerSelector) {
            """(
                ($selectorExists AND project_index.owner_login IN (
                    SELECT owner_login FROM selector_owners WHERE category_name = c.name
                ))
                OR (NOT $selectorExists AND project_index.markers && c.markers)
            )""".trimIndent()
        } else {
            "project_index.markers && c.markers"
        }
        val projectOrder = if (hasOwnerSelector) {
            """
                CASE WHEN $selectorExists THEN dependent_count END DESC NULLS LAST,
                CASE WHEN NOT $selectorExists THEN stars END DESC NULLS LAST,
                project_id ASC
            """.trimIndent()
        } else {
            "stars DESC, project_id ASC"
        }
        val categoryOrder = if (hasOwnerSelector) {
            """
                CASE WHEN $selectorExists THEN pi.dependent_count END DESC NULLS LAST,
                CASE WHEN NOT $selectorExists THEN pi.stars END DESC NULLS LAST,
                pi.project_id NULLS LAST
            """.trimIndent()
        } else {
            "pi.stars DESC NULLS LAST, pi.project_id NULLS LAST"
        }

        val sql = """
            $ownerCategoryCte
            SELECT c.id AS category_id, c.name AS category_name, c.markers AS category_markers,
                   pi.project_id, pi.owner_type, pi.owner_login, pi.repo_name, pi.name,
                   pi.stars, pi.license_name, pi.latest_version, pi.latest_version_ts,
                   array_to_string(pi.platforms, ',') AS platforms,
                   pi.plain_description, pi.tags, pi.markers, pi.targets_vector, pi.dependent_count, pi.health_score
            FROM category c
            LEFT JOIN LATERAL (
                SELECT * FROM project_index
                WHERE $projectFilter
                ORDER BY $projectOrder
                LIMIT :limit
            ) pi ON true
            ORDER BY c.id, $categoryOrder
        """.trimIndent()

        val result = linkedMapOf<Category, MutableList<SearchProjectResult>>()

        var statement = jdbcClient.sql(sql).param("limit", limit)
        ownerCategoryEntries.forEachIndexed { index, (categoryName, ownerLogin) ->
            statement = statement
                .param("selectorCategory$index", categoryName)
                .param("selectorOwner$index", ownerLogin)
        }

        statement
            .query { rs, _ ->
                val categoryName = rs.getString("category_name")
                val category = Category(
                    name = categoryName,
                    markers = rs.getArray("category_markers")?.array
                        ?.let { it as? Array<*> }?.map { it.toString() } ?: emptyList(),
                )
                val project = if (rs.getObject("project_id") != null) {
                    PROJECT_OVERVIEW_ROW_MAPPER.mapRow(rs, 0)
                } else null

                result.getOrPut(category) { mutableListOf() }.also { list ->
                    project?.let { list.add(it) }
                }
            }
            .list()

        return result
    }

    fun refreshIndex() {
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
                    .let {
                        if (it.isNullOrEmpty()) emptyList() else it.split(",").map { p -> PackagePlatform.valueOf(p) }
                    },
                markers = rs.getArray("markers")?.array?.let { it as? Array<*> }?.map { it.toString() } ?: emptyList(),
                targets = rs.getTargetsVector("targets_vector"),
                tags = rs.getArray("tags")?.array?.let { it as? Array<*> }?.map { it.toString() } ?: emptyList(),
                dependentCount = rs.getInt("dependent_count"),
                ossHealthScore = rs.getObject("health_score") as Int?,
            )
        }
    }
}