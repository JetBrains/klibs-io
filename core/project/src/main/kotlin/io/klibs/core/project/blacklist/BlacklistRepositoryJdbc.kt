package io.klibs.core.project.blacklist

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class BlacklistRepositoryJdbc(
    private val jdbcClient: JdbcClient,
    jdbcTemplate: JdbcTemplate
) : BlacklistRepository {
    companion object {
        private val logger = LoggerFactory.getLogger(BlacklistRepositoryJdbc::class.java)
    }

    private val bannedPackageInsert = SimpleJdbcInsert(jdbcTemplate)
        .withTableName("banned_packages")
        .usingGeneratedKeyColumns("id")

    override fun checkPackageBanned(groupId: String, artifactId: String): Boolean {
        val sql = """
            SELECT COUNT(*) 
            FROM banned_packages
            WHERE group_id = :groupId AND (artifact_id = :artifactId OR artifact_id IS NULL)
        """.trimIndent()

        val count = jdbcClient.sql(sql)
            .param("groupId", groupId)
            .param("artifactId", artifactId)
            .query(Int::class.java)
            .single()

        return count > 0
    }

    override fun checkPackageExists(groupId: String, artifactId: String): Boolean {
        val sql = """
            SELECT COUNT(*) 
            FROM package 
            WHERE group_id = :groupId AND artifact_id = :artifactId
        """.trimIndent()

        val count = jdbcClient.sql(sql)
            .param("groupId", groupId)
            .param("artifactId", artifactId)
            .query(Int::class.java)
            .single()

        return count > 0
    }

    override fun addToBannedPackages(groupId: String, artifactId: String?, reason: String?) {
        val params = mapOf(
            "group_id" to groupId,
            "artifact_id" to artifactId,
            "reason" to reason
        )

        bannedPackageInsert.execute(params)

        logger.debug("Added package $groupId:$artifactId to banned packages")
    }

    override fun removeBannedPackages(groupId: String, artifactId: String?) {
        val packageIds = getShouldBeBannedPackageIds(groupId, artifactId)
        deletePackages(packageIds)
    }

    override fun removeBannedPackages() {
        val packageIds = getShouldBeBannedPackageIds()
        deletePackages(packageIds)
    }

    private fun deletePackages(packageIds: List<Int>) {
        if (packageIds.isNotEmpty()) {
            deletePackageTargets(packageIds)

            val sql = """
                DELETE FROM package
                WHERE id in (:packageIds)
            """.trimIndent()

            jdbcClient.sql(sql)
                .param("packageIds", packageIds)
                .update()

            logger.debug("Removed packages: ${packageIds.joinToString(", ")}")
        }
    }

    private fun getShouldBeBannedPackageIds(groupId: String, artifactId: String?): List<Int> {
        return if (artifactId == null) {
            getShouldBeBannedPackageIdsByGroup(groupId)
        } else {
            getShouldBeBannedPackageIdsByGroupAndArtifact(groupId, artifactId)
        }
    }

    private fun getShouldBeBannedPackageIdsByGroup(groupId: String): List<Int> {
        val sql = """
            SELECT p.id
            FROM package p
            WHERE p.group_id = :groupId
        """

        return jdbcClient.sql(sql)
            .param("groupId", groupId)
            .query(Int::class.java)
            .list()
    }

    private fun getShouldBeBannedPackageIdsByGroupAndArtifact(groupId: String, artifactId: String): List<Int> {
        val sql = """
            SELECT p.id
            FROM package p
            WHERE p.group_id = :groupId
              AND p.artifact_id = :artifactId
        """

        return jdbcClient.sql(sql)
            .param("groupId", groupId)
            .param("artifactId", artifactId)
            .query(Int::class.java)
            .list()
    }

    private fun getShouldBeBannedPackageIds(): List<Int> {
        val sql = """
            SELECT p.id
            FROM package p
            INNER JOIN banned_packages bp 
              ON p.group_id = bp.group_id 
              AND (p.artifact_id = bp.artifact_id OR bp.artifact_id IS NULL);
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query(Int::class.java)
            .list()
    }

    private fun deletePackageTargets(packageIds: List<Int>) {
        if (packageIds.isEmpty()) return

        val sql = """
            DELETE FROM package_target
            WHERE package_id IN (:packageIds)
        """.trimIndent()

        jdbcClient.sql(sql)
            .param("packageIds", packageIds)
            .update()
    }
}
