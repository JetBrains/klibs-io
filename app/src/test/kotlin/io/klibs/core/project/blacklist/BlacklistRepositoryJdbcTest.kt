package io.klibs.core.project.blacklist

import BaseUnitWithDbLayerTest
import org.springframework.test.context.jdbc.Sql
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient

/**
 * Integration tests for [BlacklistRepositoryJdbc] that use a real Postgres Testcontainer
 * via [BaseUnitWithDbLayerTest]. Each test prepares DB state and executes actual queries.
 */
class BlacklistRepositoryJdbcTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var blacklistRepository: BlacklistRepositoryJdbc

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    @Sql("classpath:sql/BlacklistRepositoryJdbcTest/seed-check-exists.sql")
    fun `checkPackageExists true and false`() {
        assertTrue(blacklistRepository.checkPackageExists("com.example", "lib-a"))
        assertFalse(blacklistRepository.checkPackageExists("com.example", "lib-b"))
    }

    @Test
    @Sql("classpath:sql/BlacklistRepositoryJdbcTest/seed-banned-exact-and-group.sql")
    fun `checkPackageBanned true when exact and when group only`() {
        assertTrue(blacklistRepository.checkPackageBanned("com.ban", "exact-art"))
        assertTrue(blacklistRepository.checkPackageBanned("com.ban.all", "any-artifact"))
    }

    @Test
    fun `checkPackageBanned false when not banned`() {
        assertFalse(blacklistRepository.checkPackageBanned("com.safe", "lib"))
    }

    @Test
    @Sql("classpath:sql/BlacklistRepositoryJdbcTest/seed-remove-by-group.sql")
    fun `removeBannedPackages by group only removes all matching`() {
        blacklistRepository.removeBannedPackages("com.rm", null)

        assertTrue(countPackages("com.rm") == 0)
        assertTrue(countPackages("com.keep") == 1)
    }

    @Test
    @Sql("classpath:sql/BlacklistRepositoryJdbcTest/seed-remove-by-exact.sql")
    fun `removeBannedPackages by exact artifact removes only that package`() {
        assertTrue(countPackages("com.one") == 2)

        blacklistRepository.removeBannedPackages("com.one", "del")

        assertTrue(countPackages("com.one", "keep") == 1)
        assertTrue(countPackages("com.one", "del") == 0)
    }

    private fun countPackages(groupId: String, artifactId: String? = null): Int {
        val base = StringBuilder("SELECT COUNT(*) FROM package WHERE group_id = :g")
        if (artifactId != null) base.append(" AND artifact_id = :a")
        val spec = jdbcClient.sql(base.toString()).param("g", groupId)
        if (artifactId != null) spec.param("a", artifactId)
        return spec.query(Int::class.java).single()
    }
}
