package io.klibs.app.oss_health

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OssHealthFormulaTest {

    private companion object {
        // Defaults mirror OssHealthProperties so tests stay aligned with wired-up production config.
        const val CV_DENOMINATOR = 1.0
        const val ISSUE_MEDIAN_DAYS_THRESHOLD = 21.0
        const val PR_MEDIAN_DAYS_THRESHOLD = 5.0
        const val ACTIVE_CONTRIBUTORS_TARGET = 5
    }

    @Test
    fun `commit consistency is 1 when all weeks have identical commits`() {
        val weeks = List(12) { 7 }
        val c = OssHealthFormula.commitConsistency(weeks, CV_DENOMINATOR)
        assertNotNull(c)
        assertEquals(1.0, c, 1e-9)
    }

    @Test
    fun `commit consistency is null when the period has zero commits`() {
        assertNull(OssHealthFormula.commitConsistency(List(12) { 0 }, CV_DENOMINATOR))
    }

    @Test
    fun `commit consistency is null for an empty series`() {
        assertNull(OssHealthFormula.commitConsistency(emptyList(), CV_DENOMINATOR))
    }

    @Test
    fun `commit consistency drops with high variance`() {
        val bursty = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 60)
        val c = OssHealthFormula.commitConsistency(bursty, CV_DENOMINATOR)
        assertNotNull(c)
        // With default denominator 1.0, CV of this series is sqrt(11)/1 ~= 3.32, so clipped to 0.
        assertEquals(0.0, c, 1e-9)
    }

    @Test
    fun `commit consistency matches expected value for observed low-CV repo`() {
        // Synthetic series with CV very close to 0.56 (coil-like); sanity-checks the wiring.
        // Mean ~= 7, stddev ~= 3.9 ⇒ CV ~= 0.56
        val coilLike = listOf(5, 10, 3, 7, 12, 4, 8, 6, 11, 2, 9, 7)
        val c = OssHealthFormula.commitConsistency(coilLike, CV_DENOMINATOR)
        assertNotNull(c)
        // At denominator 1.0, we expect C to be in a reasonable mid range, not clipped.
        assertTrue(c in 0.30..0.60, "expected coil-like series to land in mid-range under denom 1.0, got $c")
    }

    @Test
    fun `commit consistency with very high CV clips to 0 at default denominator`() {
        // Synthetic series mimicking kotlinx.coroutines (CV ~= 1.27) — should clip.
        val coroutinesLike = listOf(0, 0, 0, 20, 0, 0, 25, 0, 0, 0, 30, 0)
        val c = OssHealthFormula.commitConsistency(coroutinesLike, CV_DENOMINATOR)
        assertNotNull(c)
        assertEquals(0.0, c, 1e-9)
    }

    @Test
    fun `issue responsiveness returns null for a repo with no issue activity`() {
        assertNull(
            OssHealthFormula.issueResponsiveness(
                opened = 0, closed = 0, medianCloseDays = null,
                medianDaysThreshold = ISSUE_MEDIAN_DAYS_THRESHOLD,
            )
        )
    }

    @Test
    fun `issue responsiveness is 1 at the boundary conditions`() {
        val i = OssHealthFormula.issueResponsiveness(
            opened = 100, closed = 40, // ratio = 0.4 → ratio score = 1
            medianCloseDays = 0.0,     // median = 0 → median score = 1
            medianDaysThreshold = ISSUE_MEDIAN_DAYS_THRESHOLD,
        )
        assertNotNull(i)
        assertEquals(1.0, i, 1e-9)
    }

    @Test
    fun `issue responsiveness degrades with slow close times`() {
        val i = OssHealthFormula.issueResponsiveness(
            opened = 100, closed = 40,
            medianCloseDays = ISSUE_MEDIAN_DAYS_THRESHOLD * 2,
            medianDaysThreshold = ISSUE_MEDIAN_DAYS_THRESHOLD,
        )
        assertNotNull(i)
        assertEquals(0.5, i, 1e-9)
    }

    @Test
    fun `pr management maxes out when half are merged quickly`() {
        val p = OssHealthFormula.prManagement(
            opened = 100, merged = 50, medianMergeDays = 0.0,
            medianDaysThreshold = PR_MEDIAN_DAYS_THRESHOLD,
        )
        assertNotNull(p)
        assertEquals(1.0, p, 1e-9)
    }

    @Test
    fun `pr management is 0 when no PRs are ever merged and medians are absent`() {
        val p = OssHealthFormula.prManagement(
            opened = 10, merged = 0, medianMergeDays = null,
            medianDaysThreshold = PR_MEDIAN_DAYS_THRESHOLD,
        )
        assertNotNull(p)
        assertEquals(0.0, p, 1e-9)
    }

    @Test
    fun `pr management degrades past 5-day median merge threshold`() {
        val p = OssHealthFormula.prManagement(
            opened = 100, merged = 50,
            medianMergeDays = 10.0, // beyond the tightened 5-day cliff
            medianDaysThreshold = PR_MEDIAN_DAYS_THRESHOLD,
        )
        assertNotNull(p)
        // ratio term = 1.0, median term = 0 (10d > 5d), total = 0.5
        assertEquals(0.5, p, 1e-9)
    }

    @Test
    fun `author diversity is null for a repo with no active contributors`() {
        assertNull(
            OssHealthFormula.authorDiversity(
                activeContributors = 0, topContributorShare = 0.5,
                activeContributorsTarget = ACTIVE_CONTRIBUTORS_TARGET,
            )
        )
    }

    @Test
    fun `author diversity is null when top share is unknown`() {
        assertNull(
            OssHealthFormula.authorDiversity(
                activeContributors = 3, topContributorShare = null,
                activeContributorsTarget = ACTIVE_CONTRIBUTORS_TARGET,
            )
        )
    }

    @Test
    fun `author diversity peaks for many balanced contributors`() {
        val a = OssHealthFormula.authorDiversity(
            activeContributors = 10, topContributorShare = 0.1,
            activeContributorsTarget = ACTIVE_CONTRIBUTORS_TARGET,
        )
        assertNotNull(a)
        assertTrue(a > 0.9, "expected high diversity, got $a")
    }

    @Test
    fun `author diversity penalises single-contributor projects`() {
        val a = OssHealthFormula.authorDiversity(
            activeContributors = 1, topContributorShare = 1.0,
            activeContributorsTarget = ACTIVE_CONTRIBUTORS_TARGET,
        )
        assertNotNull(a)
        assertEquals(0.12, a, 1e-9) // 0.6 * (1/5) + 0.4 * 0
    }

    @Test
    fun `final score is null when any component is missing`() {
        assertNull(OssHealthFormula.composeScore(null, 0.5, 0.5, 0.5))
        assertNull(OssHealthFormula.composeScore(0.5, null, 0.5, 0.5))
        assertNull(OssHealthFormula.composeScore(0.5, 0.5, null, 0.5))
        assertNull(OssHealthFormula.composeScore(0.5, 0.5, 0.5, null))
    }

    @Test
    fun `final score equals 100 for a perfectly healthy repo`() {
        val score = OssHealthFormula.composeScore(1.0, 1.0, 1.0, 1.0)
        assertEquals(100, score)
    }

    @Test
    fun `final score equals 0 for a totally unhealthy repo`() {
        val score = OssHealthFormula.composeScore(0.0, 0.0, 0.0, 0.0)
        assertEquals(0, score)
    }
}
