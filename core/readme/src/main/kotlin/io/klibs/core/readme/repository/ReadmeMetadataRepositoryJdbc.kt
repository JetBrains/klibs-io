package io.klibs.core.readme.repository

import io.klibs.core.readme.entity.ReadmeMetadataEntity
import org.springframework.stereotype.Repository
import org.springframework.jdbc.core.simple.JdbcClient
import kotlin.jvm.optionals.getOrNull

@Repository
class ReadmeMetadataRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ReadmeMetadataRepository {

    override fun insert(scmRepoId: Int): Boolean {
        val updated = jdbcClient.sql(
            """
            INSERT INTO readme_metadata (scm_repo_id, last_synced_at, last_processed_at)
            VALUES (:scmRepoId, current_timestamp, current_timestamp)
            """.trimIndent()
        )
            .param("scmRepoId", scmRepoId)
            .update()
        return updated == 1
    }

    override fun updateLastSyncedAt(scmRepoId: Int): Boolean {
        val updated = jdbcClient.sql(
            """
            UPDATE readme_metadata 
            SET last_synced_at = current_timestamp
            WHERE scm_repo_id = :scmRepoId
            """.trimIndent()
        )
            .param("scmRepoId", scmRepoId)
            .update()
        return updated == 1
    }

    override fun updateLastSyncedAndProcessedAt(scmRepoId: Int): Boolean {
        val updated = jdbcClient.sql(
            """
            UPDATE readme_metadata 
            SET last_synced_at = current_timestamp,
                last_processed_at = current_timestamp
            WHERE scm_repo_id = :scmRepoId
            """.trimIndent()
        )
            .param("scmRepoId", scmRepoId)
            .update()
        return updated == 1
    }

    override fun findByScmRepoId(scmRepoId: Int): ReadmeMetadataEntity? {
        return jdbcClient.sql("SELECT * FROM readme_metadata WHERE scm_repo_id = :scmRepoId")
            .param("scmRepoId", scmRepoId)
            .query(ReadmeMetadataEntity::class.java)
            .optional()
            .getOrNull()
    }

}