package io.klibs.core.search.suggest

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class SuggestRepositoryJdbc(
    private val jdbcClient: JdbcClient
): SuggestRepository {
    override fun suggestWords(query: String?, limit: Int): List<String> {
        query ?: return emptyList()

        val modifiedQuery = "%$query%"

        val sql = """
            SELECT word
            FROM suggestion_words
            WHERE word ILIKE :query
            ORDER BY word
            LIMIT :limit;
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("query", modifiedQuery)
            .param("limit", limit)
            .query(String::class.java)
            .list()
    }

    override fun refreshIndex() {
        jdbcClient.sql("REFRESH MATERIALIZED VIEW suggestion_words")
            .update()
    }
}