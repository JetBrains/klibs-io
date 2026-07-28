package io.klibs.core.search.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port-fidelity checks for the scorer and the answer-key resources (KTL-4710). */
class ScorerTest {

    @Test
    fun `nDCG rewards higher rank of the relevant lib`() {
        val rel = mapOf("koin" to 2)
        assertEquals(1.0, Scorer.ndcgAt(results("koin"), rel)!!, 1e-4)
        assertEquals(0.6309, Scorer.ndcgAt(results("x", "koin"), rel)!!, 1e-4)
        assertEquals(0.0, Scorer.ndcgAt(results("x", "y"), rel)!!, 1e-4)
    }

    @Test
    fun `graded gains put primary above secondary in ideal ordering`() {
        val rel = Scorer.relevanceGrades(case("B1", EvalClass.B, PassSpec.RankLe(1), expected = listOf("a"), also = listOf("b")))
        assertEquals(mapOf("a" to 2, "b" to 1), rel)
        assertEquals(1.0, Scorer.ndcgAt(results("a", "b"), rel)!!, 1e-4)     // ideal order
        assertEquals(0.8598, Scorer.ndcgAt(results("b", "a"), rel)!!, 1e-4)  // secondary first
    }

    @Test
    fun `non_empty case has no libs to rank so nDCG is null and falls back to pass`() {
        val c = case("D1", EvalClass.D, PassSpec.NonEmpty)
        assertNull(Scorer.ndcgAt(results(), Scorer.relevanceGrades(c)))
        assertEquals(1.0, Scorer.scoreCase(c, results("anything")).ndcg, 1e-9)
        assertEquals(0.0, Scorer.scoreCase(c, results()).ndcg, 1e-9)
    }

    @Test
    fun `pass criteria per type`() {
        assertTrue(Scorer.scoreCase(case("A1", EvalClass.A, PassSpec.RankLe(1), listOf("koin")), results("koin")).pass)
        assertFalse(Scorer.scoreCase(case("A1", EvalClass.A, PassSpec.RankLe(1), listOf("koin")), results("x", "koin")).pass)
        assertTrue(Scorer.scoreCase(case("B1", EvalClass.B, PassSpec.AnyInTop(5), listOf("koin")), results("a", "b", "koin")).pass)

        val prec = case("B2", EvalClass.B, PassSpec.PrecisionAt(5, 0.4), expected = listOf("a", "b"))
        assertTrue(Scorer.scoreCase(prec, results("a", "b", "x", "y", "z")).pass)   // 2/5 = 0.4
        assertFalse(Scorer.scoreCase(prec, results("a", "x", "y", "z", "w")).pass)  // 1/5 = 0.2
    }

    @Test
    fun `all_support_platform requires every top-k result to support the filter`() {
        val c = case("D2", EvalClass.D, PassSpec.AllSupportPlatform(2), platforms = listOf("js"))
        val supported = listOf(SearchResult("a", setOf("js")), SearchResult("b", setOf("js", "jvm")))
        val violating = listOf(SearchResult("a", setOf("js")), SearchResult("b", setOf("jvm")))
        assertTrue(Scorer.scoreCase(c, supported).pass)
        assertFalse(Scorer.scoreCase(c, violating).pass)
    }

    @Test
    fun `headline is class-weighted mean of case-weighted class means`() {
        val cases = listOf(
            case("A1", EvalClass.A, PassSpec.RankLe(1), listOf("koin"), weight = 4.0),
            case("A2", EvalClass.A, PassSpec.RankLe(1), listOf("ktor"), weight = 1.0),
        )
        val byKey = mapOf("A1" to results("koin"), "A2" to results("x", "ktor"))
        val report = Scorer.aggregate(cases.map { Scorer.scoreCase(it, byKey.getValue(it.id)) })
        // class A mean = (4*1.0 + 1*0.6309) / 5 = 0.9262 ; only class A present -> headline == class mean
        assertEquals(0.9262, report.headline, 1e-3)
        assertEquals(0.9262, report.byClass.getValue(EvalClass.A).meanNdcg, 1e-3)
    }

    @Test
    fun `a human-graded case pulls twice as hard as an ungraded one of equal traffic`() {
        // Same traffic, same results: the graded case's perfect score outweighs the ungraded miss.
        val graded = listOf(
            case("B1", EvalClass.B, PassSpec.RankLe(1), listOf("koin"), weight = 1.0, graded = true),
            case("B2", EvalClass.B, PassSpec.RankLe(1), listOf("ktor"), weight = 1.0),
        )
        val byKey = mapOf("B1" to results("koin"), "B2" to results("x", "ktor"))
        val score = { cases: List<EvalCase> ->
            Scorer.aggregate(cases.map { Scorer.scoreCase(it, byKey.getValue(it.id)) }).headline
        }
        // graded 2x on the winner: (2*1.0 + 1*0.6309)/3 = 0.8770 ; ungraded: (1.0 + 0.6309)/2 = 0.8155
        assertEquals(0.8770, score(graded), 1e-3)
        assertEquals(0.8155, score(graded.map { it.copy(graded = false) }), 1e-3)
    }

    @Test
    fun `answer key resource loads 107 graded cases`() {
        val cases = SearchEvalData.loadCases()
        assertEquals(107, cases.size)
        assertTrue(cases.all { it.id.isNotBlank() && it.weight >= 1.0 })
    }

    @Test
    fun `regression floor references only real case ids`() {
        val ids = SearchEvalData.loadCases().map { it.id }.toSet()
        val floor = SearchEvalData.loadFloor()
        assertTrue(ids.containsAll(floor)) { "floor references unknown ids: ${floor - ids}" }
    }

    private fun results(vararg keys: String) = keys.map { SearchResult(it.lowercase(), emptySet()) }

    private fun case(
        id: String, cls: EvalClass, pass: PassSpec, expected: List<String> = emptyList(),
        also: List<String> = emptyList(), platforms: List<String> = emptyList(),
        weight: Double = 1.0, graded: Boolean = false,
    ) = EvalCase(
        id = id, cls = cls, query = "q", weight = weight, graded = graded,
        expected = expected, also = also, platforms = platforms, pass = pass,
    )
}
