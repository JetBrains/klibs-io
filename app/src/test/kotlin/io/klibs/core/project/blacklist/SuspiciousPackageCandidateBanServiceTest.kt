package io.klibs.core.project.blacklist

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateEntity
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val SEED = "classpath:sql/SuspiciousPackageCandidateBanServiceTest/seed-candidates.sql"

@ActiveProfiles("test")
class SuspiciousPackageCandidateBanServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: SuspiciousPackageCandidateBanService

    @Autowired
    private lateinit var candidateRepository: SuspiciousPackageCandidateRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @Sql(SEED)
    fun `bans the coordinate and resolves its candidate with the reason`() {
        uut.banCandidate(50001, "loader", "io.github.suspect", "forked")

        assertEquals(listOf("forked"), banReasons("loader"))
        val candidate = candidate("loader")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals("forked", candidate.notes)
    }

    @Test
    @Sql(SEED)
    fun `a refused ban leaves the candidate unchanged`() {
        assertFailsWith<CandidateBanRefusedException> {
            uut.banCandidate(50001, "runtime", "io.github.suspect", "forked")
        }

        assertEquals(listOf("banned by hand"), banReasons("runtime"))
        val candidate = candidate("runtime")
        assertEquals(CandidateStatus.PENDING, candidate.status)
        assertNull(candidate.notes)
    }

    @Test
    @Sql(SEED)
    fun `refuses a candidate a reviewer has already resolved`() {
        assertFailsWith<CandidateBanRefusedException> {
            uut.banCandidate(50001, "reviewed", "io.github.suspect", "forked")
        }

        assertEquals(emptyList(), banReasons("reviewed"))
        val candidate = candidate("reviewed")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals("kept by reviewer", candidate.notes)
    }

    private fun candidate(artifactId: String): SuspiciousPackageCandidateEntity =
        candidateRepository.findById(SuspiciousPackageCandidateKey(50001, artifactId, "io.github.suspect")).get()

    private fun banReasons(artifactId: String): List<String?> =
        jdbcTemplate.queryForList(
            "SELECT reason FROM banned_packages WHERE group_id = 'io.github.suspect' AND artifact_id = ?",
            String::class.java,
            artifactId,
        )
}
