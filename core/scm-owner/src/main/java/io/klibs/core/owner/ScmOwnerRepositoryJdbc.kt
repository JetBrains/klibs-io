package io.klibs.core.owner

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class ScmOwnerRepositoryJdbc(
    private val jdbcClient: JdbcClient
) : ScmOwnerRepository {
    override fun upsert(entity: ScmOwnerEntity): ScmOwnerEntity {
        val sql = """
            INSERT INTO scm_owner (login,
                                   id_native,
                                   type,
                                   name,
                                   description,
                                   homepage,
                                   twitter_handle,
                                   email,
                                   location,
                                   company,
                                   followers,
                                   updated_at)
            VALUES (:login,
                    :idGh,
                    :type,
                    :name,
                    :description,
                    :homepage,
                    :twitterHandle,
                    :email,
                    :location,
                    :company,
                    :followers,
                    current_timestamp)
            ON CONFLICT (id_native) DO UPDATE
                SET login          = :login,
                    type           = :type,
                    name           = :name,
                    description    = :description,
                    homepage       = :homepage,
                    twitter_handle = :twitterHandle,
                    email          = :email,
                    location       = :location,
                    company        = :company,
                    followers      = :followers,
                    updated_at     = current_timestamp
            RETURNING id;
        """.trimIndent()

        val id = jdbcClient.sql(sql)
            .param("idGh", entity.nativeId)
            .param("login", entity.login)
            .param("type", entity.type.serializableName)
            .param("name", entity.name)
            .param("description", entity.description)
            .param("homepage", entity.homepage)
            .param("twitterHandle", entity.twitterHandle)
            .param("email", entity.email)
            .param("location", entity.location)
            .param("company", entity.company)
            .param("followers", entity.followers)
            .query(Int::class.java)
            .single()

        return requireNotNull(findById(id)) {
            "Unable to find a freshly persisted scm owner"
        }
    }

    override fun updateLoginByNativeId(nativeId: Long, newLogin: String): Boolean {
        val sql = """
            UPDATE scm_owner 
            SET login = :newLogin 
            WHERE id_native = :nativeId
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("nativeId", nativeId)
            .param("newLogin", newLogin)
            .update() == 1
    }

    override fun findById(id: Int): ScmOwnerEntity? {
        val sql = """
            SELECT id,
                   updated_at,
                   login,
                   id_native,
                   followers,
                   type,
                   name,
                   description,
                   homepage,
                   twitter_handle,
                   email,
                   location,
                   company
            FROM scm_owner
            WHERE id = :id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("id", id)
            .query(SCM_OWNER_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findByLogin(login: String): ScmOwnerEntity? {
        val sql = """
            SELECT id,
                   updated_at,
                   login,
                   id_native,
                   followers,
                   type,
                   name,
                   description,
                   homepage,
                   twitter_handle,
                   email,
                   location,
                   company
            FROM scm_owner
            WHERE lower(login) = lower(:login)
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("login", login)
            .query(SCM_OWNER_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    override fun findForUpdate(): ScmOwnerEntity? {
        val sql = """
            SELECT id,
                   updated_at,
                   login,
                   id_native,
                   followers,
                   type,
                   name,
                   description,
                   homepage,
                   twitter_handle,
                   email,
                   location,
                   company
            FROM scm_owner
            WHERE updated_at < (current_timestamp - interval '23 hours')
            ORDER BY random()
            LIMIT 1
            FOR UPDATE
            SKIP LOCKED
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query(SCM_OWNER_ENTITY_ROW_MAPPER)
            .optional()
            .getOrNull()
    }

    private companion object {
        private val SCM_OWNER_ENTITY_ROW_MAPPER = RowMapper<ScmOwnerEntity> { rs, _ ->
            ScmOwnerEntity(
                id = rs.getInt("id"),
                login = rs.getString("login"),
                nativeId = rs.getLong("id_native"),
                type = ScmOwnerType.findBySerializableName(rs.getString("type")),
                name = rs.getString("name"),
                description = rs.getString("description"),
                location = rs.getString("location"),
                followers = rs.getInt("followers"),
                company = rs.getString("company"),
                homepage = rs.getString("homepage"),
                twitterHandle = rs.getString("twitter_handle"),
                email = rs.getString("email"),
                updatedAtTs = rs.getTimestamp("updated_at").toInstant()
            )
        }
    }
}
