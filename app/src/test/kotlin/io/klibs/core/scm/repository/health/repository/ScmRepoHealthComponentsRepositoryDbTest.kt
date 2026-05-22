package io.klibs.core.scm.repository.health.repository

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests for [ScmRepoHealthComponentsRepository] against a Testcontainer Postgres.
 * Each test seeds a single scm_repo via [classpath:sql/.../seed-repo.sql] so the
 * scm_repo_health_components row can satisfy its FK to scm_repo(id).
 *
 * Each upsert method touches a disjoint column set; the "leaves untouched" tests are the most
 * important because they are the contract that prevents the score job from clobbering values
 * the issue/PR sync wrote (and vice versa).
 */
class ScmRepoHealthComponentsRepositoryDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repo: ScmRepoHealthComponentsRepository

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `findByScmRepoId returns null when no row exists for the repo`() {
        assertNull(repo.findByScmRepoId(SCM_REPO_ID))
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `findByScmRepoId returns the row after a write`() {
        val ts = Instant.parse("2026-04-29T12:00:00Z")
        repo.setLastIssueOrPrSyncTs(SCM_REPO_ID, ts)

        val row = repo.findByScmRepoId(SCM_REPO_ID)
        assertNotNull(row)
        assertEquals(ts, row.lastIssueOrPrSyncTs)
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `setLastIssueOrPrSyncTs inserts a new row when none exists`() {
        val ts = Instant.parse("2026-04-29T12:00:00Z")
        repo.setLastIssueOrPrSyncTs(SCM_REPO_ID, ts)

        val row = repo.findByScmRepoId(SCM_REPO_ID)
        assertNotNull(row)
        assertEquals(ts, row.lastIssueOrPrSyncTs)
        // All other columns remain null on the insert path.
        assertNull(row.issueOpenedCount)
        assertNull(row.iScore)
        assertNull(row.cScore)
        assertNull(row.healthScore)
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `setLastIssueOrPrSyncTs updates only its column on an existing row`() {
        // Seed a row with both I-P and C-A columns populated.
        repo.upsertIssuePrComponents(
            scmRepoId = SCM_REPO_ID,
            issueOpenedCount = 7, issueClosedCount = 5, medianIssueCloseDays = 3.0,
            prOpenedCount = 4, prMergedCount = 2, medianPrMergeDays = 1.5,
            iScore = 0.6, pScore = 0.7,
        )
        repo.upsertScoreComponents(
            scmRepoId = SCM_REPO_ID,
            scoreRecomputedTs = Instant.parse("2026-04-20T00:00:00Z"),
            activeContributors = 3, topContributorShare = 0.4,
            cScore = 0.5, aScore = 0.8, healthScore = 65,
        )

        val newTs = Instant.parse("2026-04-29T12:00:00Z")
        repo.setLastIssueOrPrSyncTs(SCM_REPO_ID, newTs)

        val row = assertNotNull(repo.findByScmRepoId(SCM_REPO_ID))
        assertEquals(newTs, row.lastIssueOrPrSyncTs)
        // Every other column survived the partial upsert.
        assertEquals(7, row.issueOpenedCount)
        assertEquals(5, row.issueClosedCount)
        assertEquals(3.0, row.medianIssueCloseDays)
        assertEquals(0.6, row.iScore)
        assertEquals(0.7, row.pScore)
        assertEquals(0.5, row.cScore)
        assertEquals(0.8, row.aScore)
        assertEquals(65, row.healthScore)
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `upsertIssuePrComponents inserts a new row with the 9 I-P columns set`() {
        repo.upsertIssuePrComponents(
            scmRepoId = SCM_REPO_ID,
            issueOpenedCount = 7, issueClosedCount = 5, medianIssueCloseDays = 3.0,
            prOpenedCount = 4, prMergedCount = 2, medianPrMergeDays = 1.5,
            iScore = 0.6, pScore = 0.7,
        )

        val row = assertNotNull(repo.findByScmRepoId(SCM_REPO_ID))
        assertEquals(7, row.issueOpenedCount)
        assertEquals(5, row.issueClosedCount)
        assertEquals(3.0, row.medianIssueCloseDays)
        assertEquals(4, row.prOpenedCount)
        assertEquals(2, row.prMergedCount)
        assertEquals(1.5, row.medianPrMergeDays)
        assertEquals(0.6, row.iScore)
        assertEquals(0.7, row.pScore)
        // Score-side columns remain null.
        assertNull(row.cScore)
        assertNull(row.aScore)
        assertNull(row.healthScore)
        assertNull(row.activeContributors)
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `upsertIssuePrComponents leaves the score-side columns untouched on update`() {
        // Score side written first, by the (hypothetical) score job.
        repo.upsertScoreComponents(
            scmRepoId = SCM_REPO_ID,
            scoreRecomputedTs = Instant.parse("2026-04-20T00:00:00Z"),
            activeContributors = 3, topContributorShare = 0.4,
            cScore = 0.5, aScore = 0.8, healthScore = 65,
        )

        // Issue/PR-sync side overwrites later — must not clobber the score side.
        repo.upsertIssuePrComponents(
            scmRepoId = SCM_REPO_ID,
            issueOpenedCount = 11, issueClosedCount = 10, medianIssueCloseDays = 4.0,
            prOpenedCount = 8, prMergedCount = 7, medianPrMergeDays = 2.0,
            iScore = 0.65, pScore = 0.72,
        )

        val row = assertNotNull(repo.findByScmRepoId(SCM_REPO_ID))
        assertEquals(11, row.issueOpenedCount)
        assertEquals(0.65, row.iScore)
        // Score-side fields survived.
        assertEquals(3, row.activeContributors)
        assertEquals(0.4, row.topContributorShare)
        assertEquals(0.5, row.cScore)
        assertEquals(0.8, row.aScore)
        assertEquals(65, row.healthScore)
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `upsertScoreComponents inserts a new row with the 7 score-side columns set`() {
        val recomputedTs = Instant.parse("2026-04-29T12:00:00Z")
        repo.upsertScoreComponents(
            scmRepoId = SCM_REPO_ID,
            scoreRecomputedTs = recomputedTs,
            activeContributors = 5, topContributorShare = 0.2,
            cScore = 0.7, aScore = 0.6, healthScore = 70,
        )

        val row = assertNotNull(repo.findByScmRepoId(SCM_REPO_ID))
        assertEquals(recomputedTs, row.scoreRecomputedTs)
        assertEquals(5, row.activeContributors)
        assertEquals(0.2, row.topContributorShare)
        assertEquals(0.7, row.cScore)
        assertEquals(0.6, row.aScore)
        assertEquals(70, row.healthScore)
        // I-P side remains null.
        assertNull(row.issueOpenedCount)
        assertNull(row.iScore)
        assertNull(row.pScore)
    }

    @Test
    @Sql("classpath:sql/ScmRepoHealthComponentsRepositoryDbTest/seed-repo.sql")
    fun `upsertScoreComponents leaves the I-P columns untouched on update`() {
        // I-P side written first, by the (hypothetical) issue/PR sync.
        repo.upsertIssuePrComponents(
            scmRepoId = SCM_REPO_ID,
            issueOpenedCount = 7, issueClosedCount = 5, medianIssueCloseDays = 3.0,
            prOpenedCount = 4, prMergedCount = 2, medianPrMergeDays = 1.5,
            iScore = 0.6, pScore = 0.7,
        )

        // Score job overwrites later — must not clobber the I-P side.
        repo.upsertScoreComponents(
            scmRepoId = SCM_REPO_ID,
            scoreRecomputedTs = Instant.parse("2026-04-29T12:00:00Z"),
            activeContributors = 5, topContributorShare = 0.2,
            cScore = 0.7, aScore = 0.6, healthScore = 70,
        )

        val row = assertNotNull(repo.findByScmRepoId(SCM_REPO_ID))
        assertEquals(70, row.healthScore)
        // I-P fields survived.
        assertEquals(7, row.issueOpenedCount)
        assertEquals(5, row.issueClosedCount)
        assertEquals(3.0, row.medianIssueCloseDays)
        assertEquals(4, row.prOpenedCount)
        assertEquals(2, row.prMergedCount)
        assertEquals(1.5, row.medianPrMergeDays)
        assertEquals(0.6, row.iScore)
        assertEquals(0.7, row.pScore)
    }

    companion object {
        // Matches the IDs in seed-repo.sql.
        private const val SCM_REPO_ID = 700002
    }
}
