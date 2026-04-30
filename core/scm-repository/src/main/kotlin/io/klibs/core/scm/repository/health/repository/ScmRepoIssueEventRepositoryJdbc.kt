package io.klibs.core.scm.repository.health.repository

import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventEntity
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class ScmRepoIssueEventRepositoryJdbc(
    private val jdbcClient: JdbcClient,
) : ScmRepoIssueEventRepository {

    override fun upsert(entity: ScmRepoIssueEventEntity) {
        upsertAll(listOf(entity))
    }

    override fun upsertAll(entities: Collection<ScmRepoIssueEventEntity>) {
        if (entities.isEmpty()) return
        val sql = """
            INSERT INTO scm_repo_issue_event (scm_repo_id,
                                              gh_number,
                                              type,
                                              created_at,
                                              closed_at,
                                              merged_at,
                                              duration_days)
            VALUES (:scmRepoId,
                    :ghNumber,
                    :type,
                    :createdAt,
                    :closedAt,
                    :mergedAt,
                    :durationDays)
            ON CONFLICT (scm_repo_id, gh_number) DO UPDATE SET type          = :type,
                                                               created_at    = :createdAt,
                                                               closed_at     = :closedAt,
                                                               merged_at     = :mergedAt,
                                                               duration_days = :durationDays;
        """.trimIndent()

        entities.forEach { entity ->
            jdbcClient.sql(sql)
                .param("scmRepoId", entity.scmRepoId)
                .param("ghNumber", entity.ghNumber)
                .param("type", entity.type.name)
                .param("createdAt", Timestamp.from(entity.createdAt))
                .param("closedAt", entity.closedAt?.let { Timestamp.from(it) })
                .param("mergedAt", entity.mergedAt?.let { Timestamp.from(it) })
                .param("durationDays", entity.durationDays)
                .update()
        }
    }

    override fun pruneOlderThan(scmRepoId: Int, olderThan: Instant): Int {
        val sql = """
            DELETE FROM scm_repo_issue_event
            WHERE scm_repo_id = :scmRepoId
              AND created_at < :olderThan
              AND (closed_at IS NULL OR closed_at < :olderThan)
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("olderThan", Timestamp.from(olderThan))
            .update()
    }

    override fun aggregate(scmRepoId: Int, windowStart: Instant): ScmRepoIssueEventRepository.WindowAggregates {
        val sql = """
            SELECT
                COUNT(*) FILTER (WHERE type = 'ISSUE' AND created_at >= :windowStart)               AS issue_opened_count,
                COUNT(*) FILTER (WHERE type = 'ISSUE' AND closed_at IS NOT NULL
                                   AND closed_at >= :windowStart)                                   AS issue_closed_count,
                percentile_cont(0.5) WITHIN GROUP (ORDER BY duration_days)
                    FILTER (WHERE type = 'ISSUE' AND closed_at IS NOT NULL
                              AND closed_at >= :windowStart)                                        AS median_issue_close_days,
                COUNT(*) FILTER (WHERE type = 'PR' AND created_at >= :windowStart)                  AS pr_opened_count,
                COUNT(*) FILTER (WHERE type = 'PR' AND merged_at IS NOT NULL
                                   AND merged_at >= :windowStart)                                   AS pr_merged_count,
                percentile_cont(0.5) WITHIN GROUP (ORDER BY duration_days)
                    FILTER (WHERE type = 'PR' AND merged_at IS NOT NULL
                              AND merged_at >= :windowStart)                                        AS median_pr_merge_days
            FROM scm_repo_issue_event
            WHERE scm_repo_id = :scmRepoId
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("windowStart", Timestamp.from(windowStart))
            .query(DataClassRowMapper(ScmRepoIssueEventRepository.WindowAggregates::class.java))
            .single()
    }
}
