package io.klibs.core.project.repository

import io.klibs.core.project.tags.TagData
import io.klibs.core.project.tags.TagStatisticsDTO
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class TagRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : TagRepository {
    override fun getTagStatistics(limit: Int): TagStatisticsDTO {
        val totalProjectCount = getTotalProjectCount()
        val wordOccurrences = getWordOccurrences(jdbcClient, limit)

        return TagStatisticsDTO(
            totalProjectsCount = totalProjectCount,
            tags = wordOccurrences.map { TagData(it.key, it.value) }
        )
    }

    override fun getTagsByProjectId(projectId: Int): List<String> {
        val sql = """
            SELECT unnest(tags) AS tag
            FROM project_index
            WHERE project_id = :id AND tags IS NOT NULL
        """

        return jdbcClient.sql(sql)
            .param("id", projectId)
            .query(String::class.java)
            .list()
    }

    fun getTotalProjectCount(): Long {
        val sql = """
            SELECT count(*)
            FROM project_index
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query(Long::class.java)
            .single()
    }

    fun getWordOccurrences(jdbcClient: JdbcClient, limit: Int): Map<String, Long> {
        val sql =
            """
            SELECT w.word, COUNT(*) AS occurrences
            FROM project_index, unnest(tags) AS w(word)
            WHERE tags IS NOT NULL and array_length(tags, 1) > 0
            GROUP BY w.word
            ORDER BY occurrences DESC
            LIMIT :limit
            """.trimIndent()

        val results: List<Pair<String, Long>> = jdbcClient.sql(sql)
            .param("limit", limit)
            .query { rs, _ ->
                rs.getString("word") to rs.getLong("occurrences")
            }
            .list()

        return results.associate { it }
    }
}