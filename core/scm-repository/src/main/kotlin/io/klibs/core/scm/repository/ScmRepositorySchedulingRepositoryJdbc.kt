package io.klibs.core.scm.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class ScmRepositorySchedulingRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ScmRepositorySchedulingRepository {

    override fun find(scmRepoId: Int): ScmRepositorySchedulingData? {
        val sql = """
            SELECT scm_repo_id, next_retry_at, retry_attempts
            FROM scm_repo_scheduling
            WHERE scm_repo_id = :scmRepoId
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .query(ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun clearSchedule(scmRepoId: Int) {
        jdbcClient.sql("DELETE FROM scm_repo_scheduling WHERE scm_repo_id = :scmRepoId")
            .param("scmRepoId", scmRepoId)
            .update()
    }

    override fun scheduleNextRetry(scmRepoId: Int, attempts: Int, backoffDelaySeconds: Long) {
        val sql = """
            INSERT INTO scm_repo_scheduling (scm_repo_id, next_retry_at, retry_attempts)
            VALUES (:scmRepoId, current_timestamp + (:backoffDelay * interval '1 second'), :attempts)
            ON CONFLICT (scm_repo_id) DO UPDATE
                SET next_retry_at  = current_timestamp + (:backoffDelay * interval '1 second'),
                    retry_attempts = :attempts
        """.trimIndent()

        jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("backoffDelay", backoffDelaySeconds)
            .param("attempts", attempts)
            .update()
    }

    private companion object {
        private val ROW_MAPPER = RowMapper<ScmRepositorySchedulingData> { rs, _ ->
            ScmRepositorySchedulingData(
                scmRepoId = rs.getInt("scm_repo_id"),
                nextRetryAt = rs.getTimestamp("next_retry_at")?.toInstant(),
                retryAttempts = rs.getInt("retry_attempts")
            )
        }
    }
}
