package io.klibs.core.project.repository

import io.klibs.core.project.ProjectEntity
import org.hibernate.type.SqlTypes
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Repository
class ProjectRepositoryJdbc(
    private val jdbcClient: JdbcClient,

    jdbcTemplate: JdbcTemplate,
) : ProjectRepository {

    private val projectInsert = SimpleJdbcInsert(jdbcTemplate)
        .withTableName("project")
        .usingGeneratedKeyColumns("id")

    override fun insert(projectEntity: ProjectEntity): ProjectEntity {
        val params = MapSqlParameterSource()
            .addValue("scm_repo_id", projectEntity.scmRepoId)
            .addValue("description", projectEntity.description)
            .addValue("latest_version", projectEntity.latestVersion)
            .addValue("latest_version_ts", Timestamp.from(projectEntity.latestVersionTs))

        val id = projectInsert.executeAndReturnKey(params).toInt()
        return projectEntity.copy(id = id)
    }

    override fun updateLatestVersion(id: Int, latestVersion: String, latestVersionTs: Instant): ProjectEntity {
        val sql = """
            UPDATE project 
            SET latest_version = :latestVersion, 
                latest_version_ts = :latestVersionTs 
            WHERE id = :id
        """.trimIndent()

        jdbcClient.sql(sql)
            .param("id", id)
            .param("latestVersion", latestVersion)
            .param("latestVersionTs", Timestamp.from(latestVersionTs))
            .update()

        return requireNotNull(findById(id)) {
            "Unable to find a project with id: $id"
        }
    }

    override fun updateDescription(projectName: String, ownerLogin: String, description: String) {
        val sql = """
            UPDATE project 
            SET description = :description 
            FROM scm_repo 
            JOIN scm_owner ON scm_repo.owner_id = scm_owner.id
            WHERE scm_repo.name = :projectName 
              AND scm_owner.login = :ownerLogin
              AND project.scm_repo_id = scm_repo.id
        """.trimIndent()

        val updated = jdbcClient.sql(sql)
            .param("projectName", projectName)
            .param("ownerLogin", ownerLogin)
            .param("description", description)
            .update()

        require(updated == 1) {
            "Did not update the project description for projectName: $projectName, ownerLogin: $ownerLogin"
        }
    }

    override fun updateDescription(id: Int, description: String) {
        val sql = """
            UPDATE project 
            SET description = :description 
            WHERE id = :id
        """.trimIndent()

        val updated = jdbcClient.sql(sql)
            .param("id", id)
            .param("description", description)
            .update()

        require(updated == 1) {
            "Did not update the project description of id: $id"
        }
    }

    override fun findById(id: Int): ProjectEntity? {
        val sql = """
            SELECT id,
                   scm_repo_id,
                   description,
                   latest_version,
                   latest_version_ts
            FROM project
            WHERE id = :id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("id", id)
            .query(PROJECT_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findByScmRepoId(scmRepoId: Int): ProjectEntity? {
        val sql = """
            SELECT id,
                   scm_repo_id,
                   description,
                   latest_version,
                   latest_version_ts
            FROM project
            WHERE scm_repo_id = :scmRepoId
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .query(PROJECT_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findWithoutDescription(): ProjectEntity? {
        val sql = """
            SELECT project.id,
                   project.scm_repo_id,
                   project.description,
                   project.latest_version,
                   project.latest_version_ts
            FROM project
                     JOIN scm_repo repo on project.scm_repo_id = repo.id
            WHERE repo.has_readme = true
              AND project.description IS NULL
            ORDER BY random()
            LIMIT 1
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query(PROJECT_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findWithoutTags(): ProjectEntity? {
        val sql = """
            SELECT project.id,
                   project.scm_repo_id,
                   project.description,
                   project.latest_version,
                   project.latest_version_ts
            FROM project
                     JOIN scm_repo repo on project.scm_repo_id = repo.id
            WHERE repo.has_readme = true
              AND NOT EXISTS (
                    SELECT 1 FROM project_tags pt WHERE pt.project_id = project.id
              )
            ORDER BY random()
            LIMIT 1
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query(PROJECT_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findProjectsByPackages(
        groupId: String,
        artifactId: String?
    ): Set<Int> {
        val sql =
            """
            SELECT distinct project.id
            FROM project
                     JOIN package on project.id = package.project_id
            WHERE package.group_id = :groupId
              AND (:artifactId is NULL or package.artifact_id = :artifactId)
            """.trimIndent()

        return jdbcClient.sql(sql)
            .param("groupId", groupId)
            .param("artifactId", artifactId, SqlTypes.VARCHAR)
            .query { rs, _ -> rs.getInt("id") }
            .set()
    }

    private companion object {
        private val PROJECT_ENTITY_ROW_MAPPER = RowMapper<ProjectEntity> { rs, _ ->
            ProjectEntity(
                id = rs.getInt("id"),
                scmRepoId = rs.getInt("scm_repo_id"),
                description = rs.getString("description"),
                latestVersion = rs.getString("latest_version"),
                latestVersionTs = rs.getTimestamp("latest_version_ts").toInstant(),
            )
        }
    }
}
