package io.klibs.app.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateEntity
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import io.klibs.core.pckg.service.SuspiciousPackageCandidateCollectionService
import io.klibs.integration.github.GitHubIntegration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val SEED = "classpath:sql/SuspiciousForkBanServiceTest/seed-candidates.sql"
private const val SUSPECT_REASON = "Auto-banned: suspect/zipline is a fork of cashapp/zipline"

@ActiveProfiles("test")
class SuspiciousForkBanServiceTest : BaseUnitWithDbLayerTest() {

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @Autowired
    private lateinit var uut: SuspiciousForkBanService

    @Autowired
    private lateinit var collectionService: SuspiciousPackageCandidateCollectionService

    @Autowired
    private lateinit var candidateRepository: SuspiciousPackageCandidateRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun stubGitHub() {
        stubFork("suspect")
        stubFork("suspect-a")
        stubFork("suspect-b")
        whenever(gitHubIntegration.getForkParentFullName("flaky", "zipline"))
            .thenAnswer { throw IOException("rate limited") }
    }

    @Test
    @Sql(SEED)
    fun `bans a confirmed fork and resolves its candidate with the same reason`() {
        uut.banConfirmedForks()

        assertEquals(SUSPECT_REASON, banReason("io.github.suspect", "loader"))
        val candidate = candidate("loader", "io.github.suspect")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals(SUSPECT_REASON, candidate.notes)
        assertEquals(0, packageCount("io.github.suspect", "loader"))
    }

    @Test
    @Sql(SEED)
    fun `a dependant inside the same project does not block the ban`() {
        uut.banConfirmedForks()

        assertEquals(SUSPECT_REASON, banReason("io.github.suspect", "runtime"))
    }

    @Test
    @Sql(SEED)
    fun `never bans a candidate depended on from another project`() {
        uut.banConfirmedForks()

        assertNull(banReason("io.github.suspect", "depended"))
        assertEquals(CandidateStatus.PENDING, candidate("depended", "io.github.suspect").status)
    }

    @Test
    @Sql(SEED)
    fun `leaves a candidate pending when its owner holds no fork`() {
        uut.banConfirmedForks()

        assertNull(banReason("io.github.renamed", "core"))
        assertEquals(CandidateStatus.PENDING, candidate("core", "io.github.renamed").status)
    }

    @Test
    @Sql(SEED)
    fun `leaves a candidate pending when its owner's fork has another parent`() {
        whenever(gitHubIntegration.getForkParentFullName("suspect", "zipline")).thenReturn("someone-else/zipline")

        uut.banConfirmedForks()

        assertNull(banReason("io.github.suspect", "loader"))
        assertEquals(CandidateStatus.PENDING, candidate("loader", "io.github.suspect").status)
    }

    @Test
    @Sql(SEED)
    fun `matches the fork's parent ignoring case`() {
        whenever(gitHubIntegration.getForkParentFullName("suspect", "zipline")).thenReturn("CashApp/Zipline")

        uut.banConfirmedForks()

        assertEquals(SUSPECT_REASON, banReason("io.github.suspect", "loader"))
    }

    @Test
    @Sql(SEED)
    fun `never overrides a reviewer's decision`() {
        uut.banConfirmedForks()

        assertNull(banReason("io.github.suspect", "reviewed"))
        val candidate = candidate("reviewed", "io.github.suspect")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals("kept by reviewer", candidate.notes)
    }

    @Test
    @Sql(SEED)
    fun `bans only the evaluated coordinates of a republisher`() {
        uut.banConfirmedForks()

        assertNull(banReason("io.github.suspect", "extra"))
        assertEquals(1, packageCount("io.github.suspect", "extra"))
        assertNull(groupBanReason("io.github.suspect"))
    }

    @Test
    @Sql(SEED)
    fun `bans nothing when the fork lookup fails`() {
        uut.banConfirmedForks()

        assertNull(banReason("io.github.flaky", "net"))
        assertEquals(CandidateStatus.PENDING, candidate("net", "io.github.flaky").status)
    }

    @Test
    @Sql(SEED)
    fun `bans both members of a conflict even when the first ban ends it`() {
        uut.banConfirmedForks()

        assertEquals(
            "Auto-banned: suspect-a/zipline is a fork of cashapp/zipline",
            banReason("io.github.suspect-a", "dup"),
        )
        assertEquals(
            "Auto-banned: suspect-b/zipline is a fork of cashapp/zipline",
            banReason("io.github.suspect-b", "dup"),
        )
    }

    @Test
    @Sql(SEED)
    fun `never bans a candidate whose conflict ended before the refresh`() {
        jdbcTemplate.update("DELETE FROM package WHERE group_id = 'app.cash.zipline' AND artifact_id = 'loader'")

        collectionService.refreshCandidates()
        uut.banConfirmedForks()

        assertNull(banReason("io.github.suspect", "loader"))
        assertEquals(1, packageCount("io.github.suspect", "loader"))
    }

    @Test
    @Sql(SEED)
    fun `a refused ban leaves its candidate unchanged and the run continues`() {
        jdbcTemplate.update("DELETE FROM package WHERE group_id = 'io.github.suspect' AND artifact_id = 'loader'")

        val summary = uut.banConfirmedForks()

        val candidate = candidate("loader", "io.github.suspect")
        assertEquals(CandidateStatus.PENDING, candidate.status)
        assertNull(candidate.notes)
        assertNull(banReason("io.github.suspect", "loader"))
        assertEquals(SUSPECT_REASON, banReason("io.github.suspect", "runtime"))
        assertEquals(3, summary.banned)
    }

    @Test
    @Sql(SEED)
    fun `keeps a non-qualifying candidate's reviewer-set status and notes`() {
        uut.banConfirmedForks()

        val candidate = candidate("runtime", "app.cash.zipline")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals("the original", candidate.notes)
    }

    @Test
    @Sql(SEED)
    fun `reports each outcome`() {
        val summary = uut.banConfirmedForks()

        assertEquals(ForkBanSummary(evaluated = 6, banned = 4, notFork = 1, noDecision = 1), summary)
    }

    @Test
    @Sql(SEED)
    fun `looks up each suspect repository once per run`() {
        uut.banConfirmedForks()

        verify(gitHubIntegration, times(1)).getForkParentFullName("suspect", "zipline")
    }

    @Test
    @Sql(SEED)
    fun `a second run bans nothing new and does not look up what it already banned`() {
        uut.banConfirmedForks()
        val secondRun = uut.banConfirmedForks()

        assertEquals(0, secondRun.banned)
        assertEquals(4, bannedCount())
        verify(gitHubIntegration, times(1)).getForkParentFullName("suspect", "zipline")
        assertEquals(2, secondRun.evaluated)
    }

    private fun stubFork(owner: String) {
        whenever(gitHubIntegration.getForkParentFullName(owner, "zipline")).thenReturn("cashapp/zipline")
    }

    private fun candidate(artifactId: String, groupId: String): SuspiciousPackageCandidateEntity =
        candidateRepository.findById(SuspiciousPackageCandidateKey(49001, artifactId, groupId)).get()

    private fun banReason(groupId: String, artifactId: String): String? =
        jdbcTemplate.queryForList(
            "SELECT reason FROM banned_packages WHERE group_id = ? AND artifact_id = ?",
            String::class.java,
            groupId,
            artifactId,
        ).singleOrNull()

    private fun groupBanReason(groupId: String): String? =
        jdbcTemplate.queryForList(
            "SELECT reason FROM banned_packages WHERE group_id = ? AND artifact_id IS NULL",
            String::class.java,
            groupId,
        ).singleOrNull()

    private fun bannedCount(): Int? =
        jdbcTemplate.queryForObject("SELECT count(*) FROM banned_packages", Int::class.java)

    private fun packageCount(groupId: String, artifactId: String): Int? =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM package WHERE group_id = ? AND artifact_id = ?",
            Int::class.java,
            groupId,
            artifactId,
        )
}
