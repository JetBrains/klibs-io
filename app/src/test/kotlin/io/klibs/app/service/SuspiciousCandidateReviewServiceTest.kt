package io.klibs.app.service

import BaseUnitWithDbLayerTest
import io.klibs.app.api.KeepSuspiciousCandidateRequest
import io.klibs.app.service.SuspiciousCandidateReviewService.Companion.METRIC_SUSPICIOUS_PACKAGE_CANDIDATE_PENDING
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateEntity
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = "classpath:sql/SuspiciousCandidateReviewServiceTest/seed-candidates.sql"

@ActiveProfiles("test")
class SuspiciousCandidateReviewServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: SuspiciousCandidateReviewService

    @Autowired
    private lateinit var candidateRepository: SuspiciousPackageCandidateRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

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

    @Test
    @Sql(SEED)
    fun `pending gauge counts PENDING candidates and drops once they are kept`() {
        val pendingGauge = assertNotNull(meterRegistry.find(METRIC_SUSPICIOUS_PACKAGE_CANDIDATE_PENDING).gauge())
        assertEquals(2.0, pendingGauge.value())

        uut.keep(KeepSuspiciousCandidateRequest(50001, "io.github.suspect", "legit relocation"))

        assertEquals(0.0, pendingGauge.value())
    }

    @Test
    fun `pending gauge is NaN when the count query fails`() {
        val failingRepo = mock<SuspiciousPackageCandidateRepository> {
            whenever(it.countByStatus(CandidateStatus.PENDING)).thenThrow(RuntimeException("DB error"))
        }
        val registry = SimpleMeterRegistry()
        SuspiciousCandidateReviewService(failingRepo, mock(), registry)

        assertTrue(registry.get(METRIC_SUSPICIOUS_PACKAGE_CANDIDATE_PENDING).gauge().value().isNaN())
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
