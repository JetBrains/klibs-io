package io.klibs.core.owner

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class ScmOwnerSchedulingRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ScmOwnerSchedulingRepository {

    override fun find(scmOwnerId: Int): ScmOwnerSchedulingData? {
        val sql = """
            SELECT scm_owner_id, next_retry_at, retry_attempts, reason
            FROM scm_owner_scheduling
            WHERE scm_owner_id = :scmOwnerId
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("scmOwnerId", scmOwnerId)
            .query(ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun clearSchedule(scmOwnerId: Int) {
        jdbcClient.sql("DELETE FROM scm_owner_scheduling WHERE scm_owner_id = :scmOwnerId")
            .param("scmOwnerId", scmOwnerId)
            .update()
    }

    override fun scheduleNextRetry(
        scmOwnerId: Int,
        attempts: Int,
        backoffDelaySeconds: Long,
        reason: String
    ) {
        val sql = """
            INSERT INTO scm_owner_scheduling (scm_owner_id, next_retry_at, retry_attempts, reason)
            VALUES (:scmOwnerId, current_timestamp + (:backoffDelay * interval '1 second'), :attempts, :reason)
            ON CONFLICT (scm_owner_id) DO UPDATE
                SET next_retry_at  = current_timestamp + (:backoffDelay * interval '1 second'),
                    retry_attempts = :attempts,
                    reason         = :reason
        """.trimIndent()

        jdbcClient.sql(sql)
            .param("scmOwnerId", scmOwnerId)
            .param("backoffDelay", backoffDelaySeconds)
            .param("attempts", attempts)
            .param("reason", reason)
            .update()
    }

    private companion object {
        private val ROW_MAPPER = RowMapper<ScmOwnerSchedulingData> { rs, _ ->
            ScmOwnerSchedulingData(
                scmOwnerId = rs.getInt("scm_owner_id"),
                nextRetryAt = rs.getTimestamp("next_retry_at")?.toInstant(),
                retryAttempts = rs.getInt("retry_attempts"),
                reason = rs.getString("reason")
            )
        }
    }
}
