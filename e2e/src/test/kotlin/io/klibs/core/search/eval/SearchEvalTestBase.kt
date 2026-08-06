package io.klibs.core.search.eval

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.slf4j.LoggerFactory
import org.springframework.test.web.servlet.MockMvc
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base for both search-eval tiers: runs each [EvalCase] as its own test, grouped by
 * [EvalClass], and hands the aggregated [RunReport] to the subclass.
 *
 * A subclass supplies the corpus wiring ([mockMvc]), a [tier] label, the cases to run
 * ([casesToRun]), and what to do with the report ([onRunComplete]).
 */
abstract class SearchEvalTestBase {

    protected abstract val mockMvc: MockMvc
    protected abstract val tier: String

    /** Cases this tier runs: the eval tier runs every case, the regression tier only its floor. */
    protected abstract fun casesToRun(): List<EvalCase>

    /** Called once every case has run and the report is logged: records the floor or the run history. */
    protected abstract fun onRunComplete(report: RunReport)

    /**
     * A recording run (`-Dsearch.floor.overwrite`) exists to measure what passes right now, so a
     * failing case is data, not a defect: it aborts (skipped) instead of failing, and the run stays
     * green while [onRunComplete] writes the new floor.
     */
    protected open val isRecording: Boolean get() = false

    protected val log = LoggerFactory.getLogger(javaClass)!!

    private val outcomes = CopyOnWriteArrayList<CaseOutcome>()

    private fun assertClass(cls: EvalClass): List<DynamicTest> =
        casesToRun().filter { it.cls == cls }.map { case ->
            DynamicTest.dynamicTest(case.id) {
                val outcome = Scorer.scoreCase(case, mockMvc.searchProjects(case))
                outcomes += outcome
                if (isRecording) assumeTrue(outcome.pass) { outcome.failureMessage(tier) }
                else assertTrue(outcome.pass) { outcome.failureMessage(tier) }
            }
        }

    @TestFactory
    fun `A - exact name resolves to its project`() = assertClass(EvalClass.A)

    @TestFactory
    fun `B - category keyword surfaces the answer-key libs`() = assertClass(EvalClass.B)

    @TestFactory
    fun `C - related lib maps to a KMP equivalent`() = assertClass(EvalClass.C)

    @TestFactory
    fun `D - platform filter is respected`() = assertClass(EvalClass.D)

    @TestFactory
    fun `M - multi-term query is not diluted`() = assertClass(EvalClass.M)

    @TestFactory
    fun `E - query mechanics (dotted, typo, stop-words) work`() = assertClass(EvalClass.E)

    @AfterAll
    fun reportRun() {
        val report = Scorer.aggregate(outcomes)
        logReport(report)
        onRunComplete(report)
    }

    /** Headline plus the per-class means behind it. */
    private fun logReport(report: RunReport) {
        log.info(
            "{}: {}/{} cases pass, headline={}",
            tier, report.rawPassed, report.outcomes.size, "%.4f".format(report.headline),
        )
        report.byClass.entries.sortedBy { it.key.name }.forEach { (cls, summary) ->
            log.info(
                "  {} — {}/{} pass, mean nDCG@10={}, MRR={}",
                cls, summary.passed, summary.n, "%.4f".format(summary.meanNdcg), "%.4f".format(summary.mrr),
            )
        }
    }
}
