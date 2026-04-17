package io.klibs.core.scm.repository

import io.klibs.core.owner.ScmOwnerType
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Repository
class ScmRepositoryRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ScmRepositoryRepository {

    override fun upsert(entity: ScmRepositoryEntity): ScmRepositoryEntity {
        val sql = """
            INSERT INTO scm_repo (id_native,
                                  name,
                                  description,
                                  default_branch,
                                  created_ts,
                                  owner_id,
                                  homepage,
                                  has_gh_pages,
                                  has_issues,
                                  has_wiki,
                                  has_readme,
                                  license_key,
                                  license_name,
                                  stars,
                                  open_issues,
                                  last_activity_ts,
                                  updated_at)
            VALUES (:idGh,
                    :name,
                    :description,
                    :defaultBranch,
                    :createdTs,
                    :ownerId,
                    :homepage,
                    :hasGhPages,
                    :hasIssues,
                    :hasWiki,
                    :hasReadme,
                    :licenseKey,
                    :licenseName,
                    :stars,
                    :openIssues,
                    :lastActivityTs,
                    current_timestamp)
            ON CONFLICT (id_native) DO UPDATE SET name             = :name,
                                                  description      = :description,
                                                  default_branch   = :defaultBranch,
                                                  owner_id         = :ownerId,
                                                  homepage         = :homepage,
                                                  has_gh_pages     = :hasGhPages,
                                                  has_issues       = :hasIssues,
                                                  has_wiki         = :hasWiki,
                                                  has_readme       = :hasReadme,
                                                  license_key      = :licenseKey,
                                                  license_name     = :licenseName,
                                                  stars            = :stars,
                                                  open_issues      = :openIssues,
                                                  last_activity_ts = :lastActivityTs,
                                                  updated_at       = current_timestamp
            RETURNING id;
        """.trimIndent()

        val id = jdbcClient.sql(sql)
            .param("idGh", entity.nativeId)
            .param("name", entity.name)
            .param("description", entity.description)
            .param("defaultBranch", entity.defaultBranch)
            .param("createdTs", Timestamp.from(entity.createdTs))
            .param("ownerId", entity.ownerId)
            .param("homepage", entity.homepage)
            .param("hasGhPages", entity.hasGhPages)
            .param("hasIssues", entity.hasIssues)
            .param("hasWiki", entity.hasWiki)
            .param("hasReadme", entity.hasReadme)
            .param("licenseKey", entity.licenseKey)
            .param("licenseName", entity.licenseName)
            .param("stars", entity.stars)
            .param("openIssues", entity.openIssues)
            .param("lastActivityTs", Timestamp.from(entity.lastActivityTs))
            .query(Int::class.java)
            .single()

        return requireNotNull(findById(id)) {
            "Unable to find a freshly upserted scm repository"
        }
    }

    override fun update(entity: ScmRepositoryEntity): ScmRepositoryEntity {
        val sql = """
            UPDATE scm_repo  
            SET 
                id_native        = :idGh,
                description      = :description,
                default_branch   = :defaultBranch,
                homepage         = :homepage,
                has_gh_pages     = :hasGhPages,
                has_issues       = :hasIssues,
                has_wiki         = :hasWiki,
                has_readme       = :hasReadme,
                license_key      = :licenseKey,
                license_name     = :licenseName,
                stars            = :stars,
                open_issues      = :openIssues,
                last_activity_ts = :lastActivityTs,
                updated_at       = current_timestamp
            WHERE lower(name) = lower(:name) AND owner_id = :ownerId
            RETURNING id;
        """.trimIndent()

        val id = jdbcClient.sql(sql)
            .param("idGh", entity.nativeId)
            .param("name", entity.name)
            .param("description", entity.description)
            .param("defaultBranch", entity.defaultBranch)
            .param("createdTs", Timestamp.from(entity.createdTs))
            .param("ownerId", entity.ownerId)
            .param("homepage", entity.homepage)
            .param("hasGhPages", entity.hasGhPages)
            .param("hasIssues", entity.hasIssues)
            .param("hasWiki", entity.hasWiki)
            .param("hasReadme", entity.hasReadme)
            .param("licenseKey", entity.licenseKey)
            .param("licenseName", entity.licenseName)
            .param("stars", entity.stars)
            .param("openIssues", entity.openIssues)
            .param("lastActivityTs", Timestamp.from(entity.lastActivityTs))
            .query(Int::class.java)
            .single()

        return requireNotNull(findById(id)) {
            "Unable to find a freshly upserted scm repository"
        }
    }

    override fun setUpdatedAt(id: Int, updatedAt: Instant): Boolean {
        val sql = """
            UPDATE scm_repo 
            SET updated_at = :updatedAt 
            WHERE id = :id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("id", id)
            .param("updatedAt", Timestamp.from(updatedAt))
            .update() > 0
    }

    override fun findById(id: Int): ScmRepositoryEntity? {
        val sql = """
            SELECT repo.id,
                   repo.id_native,
                   repo.name,
                   repo.description,
                   repo.default_branch,
                   repo.created_ts,
                   repo.owner_id,
                   owner.type     owner_type,
                   owner.login AS owner_login,
                   repo.homepage,
                   repo.has_gh_pages,
                   repo.has_issues,
                   repo.has_wiki,
                   repo.has_readme,
                   repo.license_key,
                   repo.license_name,
                   repo.stars,
                   repo.open_issues,
                   repo.last_activity_ts,
                   repo.updated_at
            FROM scm_repo repo
                     JOIN scm_owner owner ON repo.owner_id = owner.id
            WHERE repo.id = :id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("id", id)
            .query(SCM_REPO_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findByNativeId(nativeId: Long): ScmRepositoryEntity? {
        val sql = """
            SELECT repo.id,
                   repo.id_native,
                   repo.name,
                   repo.description,
                   repo.default_branch,
                   repo.created_ts,
                   repo.owner_id,
                   owner.type     owner_type,
                   owner.login AS owner_login,
                   repo.homepage,
                   repo.has_gh_pages,
                   repo.has_issues,
                   repo.has_wiki,
                   repo.has_readme,
                   repo.license_key,
                   repo.license_name,
                   repo.stars,
                   repo.open_issues,
                   repo.last_activity_ts,
                   repo.updated_at
            FROM scm_repo repo
                     JOIN scm_owner owner ON repo.owner_id = owner.id
            WHERE repo.id_native = :nativeId
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("nativeId", nativeId)
            .query(SCM_REPO_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findByName(ownerLogin: String, name: String): ScmRepositoryEntity? {
        val sql = """
            SELECT repo.id,
                   repo.id_native,
                   repo.name,
                   repo.description,
                   repo.default_branch,
                   repo.created_ts,
                   repo.owner_id,
                   owner.type     owner_type,
                   owner.login AS owner_login,
                   repo.homepage,
                   repo.has_gh_pages,
                   repo.has_issues,
                   repo.has_wiki,
                   repo.has_readme,
                   repo.license_key,
                   repo.license_name,
                   repo.stars,
                   repo.open_issues,
                   repo.last_activity_ts,
                   repo.updated_at
            FROM scm_repo repo
                     JOIN scm_owner owner ON repo.owner_id = owner.id
            WHERE lower(owner.login) = lower(:ownerLogin)
              AND lower(repo.name) = lower(:name)
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("ownerLogin", ownerLogin)
            .param("name", name)
            .query(SCM_REPO_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findIdByName(ownerLogin: String, name: String): Int? {
        val sql = """
            SELECT repo.id
            FROM scm_repo repo
                     JOIN scm_owner owner ON repo.owner_id = owner.id
            WHERE lower(owner.login) = lower(:ownerLogin)
              AND lower(repo.name) = lower(:name)
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("ownerLogin", ownerLogin)
            .param("name", name)
            .query { rs, _ -> rs.getInt(1) }
            .optional()
            .getOrNull()
    }

    override fun findMultipleForUpdate(limit: Int): List<ScmRepositoryEntity> {
        val sql = """
            SELECT repo.id,
                   repo.id_native,
                   repo.name,
                   repo.description,
                   repo.default_branch,
                   repo.created_ts,
                   repo.owner_id,
                   owner.type     owner_type,
                   owner.login AS owner_login,
                   repo.homepage,
                   repo.has_gh_pages,
                   repo.has_issues,
                   repo.has_wiki,
                   repo.has_readme,
                   repo.license_key,
                   repo.license_name,
                   repo.stars,
                   repo.open_issues,
                   repo.last_activity_ts,
                   repo.updated_at
            FROM scm_repo repo
                     JOIN scm_owner owner ON repo.owner_id = owner.id
            WHERE repo.updated_at < (current_timestamp - interval '24 hours')
            ORDER BY repo.stars DESC
            LIMIT :limit FOR UPDATE
                SKIP LOCKED
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("limit", limit)
            .query(SCM_REPO_ENTITY_ROW_MAPPER)
            .list()
    }

    private companion object {
        private val SCM_REPO_ENTITY_ROW_MAPPER = RowMapper<ScmRepositoryEntity> { rs, _ ->
            ScmRepositoryEntity(
                id = rs.getInt("id"),
                nativeId = rs.getLong("id_native"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                defaultBranch = rs.getString("default_branch"),
                createdTs = rs.getTimestamp("created_ts").toInstant(),
                ownerId = rs.getInt("owner_id"),
                ownerType = ScmOwnerType.findBySerializableName(rs.getString("owner_type")),
                ownerLogin = rs.getString("owner_login"),
                homepage = rs.getString("homepage"),
                hasGhPages = rs.getBoolean("has_gh_pages"),
                hasIssues = rs.getBoolean("has_issues"),
                hasWiki = rs.getBoolean("has_wiki"),
                hasReadme = rs.getBoolean("has_readme"),
                licenseKey = rs.getString("license_key"),
                licenseName = rs.getString("license_name"),
                stars = rs.getInt("stars"),
                openIssues = rs.getInt("open_issues"),
                lastActivityTs = rs.getTimestamp("last_activity_ts").toInstant(),
                updatedAtTs = rs.getTimestamp("updated_at").toInstant()
            )
        }
    }
}
