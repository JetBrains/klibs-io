package io.klibs.app.util.instant

import org.springframework.dao.DataAccessException
import org.springframework.dao.NonTransientDataAccessException
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class InstantRepositoryJdbc(private val jdbcTemplate: JdbcTemplate): InstantRepository {
    private val TABLE_NAME = "last_package_indexed"

    // Updates an Instant in the database
    override fun save(instant: Instant) {
        val sql = "UPDATE $TABLE_NAME SET instant_value = ?"
        jdbcTemplate.update(sql, Timestamp.from(instant))
    }

    // Retrieve the latest Instant from the database
    override fun retrieveLatest(): Instant? {
        val sql = "SELECT instant_value FROM $TABLE_NAME ORDER BY instant_value DESC LIMIT 1"
        return try {
            jdbcTemplate.queryForObject(sql) { rs, _ ->
                rs.getTimestamp("instant_value").toInstant()
            }
        }
        catch (e: DataAccessException) {
            null
        }
    }
}