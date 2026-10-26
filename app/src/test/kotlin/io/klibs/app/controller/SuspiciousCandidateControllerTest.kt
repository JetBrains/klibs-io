package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import io.klibs.app.api.ReviewSuspiciousCandidateDecision
import io.klibs.app.api.ReviewSuspiciousCandidatesRequest
import io.klibs.app.api.SuspiciousCandidateResponse
import io.klibs.app.api.SuspiciousCandidateDecision
import io.klibs.app.api.SuspiciousCandidateReviewOutcome
import io.klibs.app.api.SuspiciousCandidateReviewResult
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals

private const val SEED = "classpath:sql/SuspiciousCandidateControllerTest/seed-candidates.sql"
private const val SUSPECT = "io.github.suspect"

@ActiveProfiles("test")
class SuspiciousCandidateControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var candidateRepository: SuspiciousPackageCandidateRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @Sql(SEED)
    fun `lists only pending candidates`() {
        val response = pending("")

        assertEquals(
            setOf("app.cash.zipline:loader", "$SUSPECT:loader", "$SUSPECT:runtime"),
            response.map { "${it.groupId}:${it.artifactId}" }.toSet(),
        )
        assertEquals(setOf(60001), response.map { it.projectId }.toSet())
    }

    @Test
    @Sql(SEED)
    fun `pages split pending candidates in primary key order`() {
        val coordinates = { page: Int -> pending("?page=$page&limit=2").map { "${it.groupId}:${it.artifactId}" } }

        assertEquals(listOf("app.cash.zipline:loader", "$SUSPECT:loader"), coordinates(1))
        assertEquals(listOf("$SUSPECT:runtime"), coordinates(2))
        assertEquals(emptyList(), coordinates(3))
    }

    @Test
    @Sql(SEED)
    fun `out of range page or limit is rejected`() {
        mockMvc.get("/suspicious-candidates?page=0").andExpect { status { isBadRequest() } }
        mockMvc.get("/suspicious-candidates?limit=0").andExpect { status { isBadRequest() } }
        mockMvc.get("/suspicious-candidates?limit=1001").andExpect { status { isBadRequest() } }
    }

    @Test
    @Sql(SEED)
    fun `blank or too long notes are rejected`() {
        reviewRequest(listOf(decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("runtime"), " ")))
            .andExpect { status { isBadRequest() } }
        reviewRequest(
            listOf(decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("runtime"), "x".repeat(256)))
        ).andExpect { status { isBadRequest() } }

        assertEquals(CandidateStatus.PENDING to null, candidate("runtime", SUSPECT))
        assertEquals(0, bannedCount())
    }

    @Test
    @Sql(SEED)
    fun `review keep skips when an unreviewed pending artifact exists`() {
        val results = review(
            listOf(decision(SUSPECT, SuspiciousCandidateDecision.KEEP, listOf("loader")))
        )

        assertEquals(listOf(SuspiciousCandidateReviewOutcome.SKIP), results.map { it.outcome })
        assertEquals("runtime", results.single().message.substringAfter("[").substringBefore("]"))
        assertEquals(CandidateStatus.PENDING to null, candidate("loader", SUSPECT))
        assertEquals(CandidateStatus.PENDING to null, candidate("runtime", SUSPECT))
    }

    @Test
    @Sql(SEED)
    fun `review keep resolves only reviewed group pending artifacts`() {
        val body = """
            {
              "generatedAt": "2026-01-01T00:00:00Z",
              "decisions": [{
                "projectId": 60001, "groupId": "$SUSPECT", "decision": "KEEP",
                "artifacts": ["loader", "runtime"], "note": "Reviewed keep",
                "source": "human", "reviewerNote": "Reviewed keep", "repo": "cashapp/zipline"
              }]
            }
        """.trimIndent()
        val results = reviewJson(body)

        assertEquals(SuspiciousCandidateReviewOutcome.OK, results.single().outcome)
        assertEquals(CandidateStatus.RESOLVED to "Reviewed keep", candidate("loader", SUSPECT))
        assertEquals(CandidateStatus.RESOLVED to "Reviewed keep", candidate("runtime", SUSPECT))
        assertEquals(0, bannedCount())
    }

    @Test
    @Sql(SEED)
    fun `review ban applies once per artifact`() {
        val results = review(
            listOf(decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("loader", "runtime")))
        )

        assertEquals(listOf("loader", "runtime"), results.map { it.artifactId })
        assertEquals(listOf(SuspiciousCandidateReviewOutcome.OK, SuspiciousCandidateReviewOutcome.OK), results.map { it.outcome })
        assertEquals(listOf("loader", "runtime"), bannedArtifacts().filterNotNull().sorted())
        assertEquals(CandidateStatus.RESOLVED to "Reviewed by human", candidate("loader", SUSPECT))
    }

    @Test
    @Sql(SEED)
    fun `review ban skips resolved artifacts and reports new pending artifacts`() {
        val results = review(
            listOf(decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("reviewed")))
        )

        assertEquals(
            listOf("reviewed", "loader", "runtime"), results.map { it.artifactId }
        )
        assertEquals(List(3) { SuspiciousCandidateReviewOutcome.SKIP }, results.map { it.outcome })
        assertEquals(CandidateStatus.RESOLVED to "kept by reviewer", candidate("reviewed", SUSPECT))
        assertEquals(CandidateStatus.PENDING to null, candidate("loader", SUSPECT))
        assertEquals(CandidateStatus.PENDING to null, candidate("runtime", SUSPECT))
        assertEquals(0, bannedCount())
    }

    @Test
    @Sql(SEED)
    fun `review processes keeps before bans and returns per artifact results`() {
        val results = review(
            listOf(
                decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("loader", "runtime")),
                decision("app.cash.zipline", SuspiciousCandidateDecision.KEEP, listOf("loader")),
            )
        )

        assertEquals(
            listOf(
                SuspiciousCandidateDecision.KEEP,
                SuspiciousCandidateDecision.BAN,
                SuspiciousCandidateDecision.BAN,
            ),
            results.map { it.decision },
        )
        assertEquals(
            listOf("app.cash.zipline", SUSPECT, SUSPECT), results.map { it.groupId }
        )
        assertEquals(List(3) { SuspiciousCandidateReviewOutcome.OK }, results.map { it.outcome })
    }

    @Test
    @Sql(SEED)
    fun `review rejects duplicate groups and invalid artifacts or notes before writing`() {
        reviewRequest(
            listOf(
                decision(SUSPECT, SuspiciousCandidateDecision.KEEP, listOf("loader", "runtime")),
                decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("loader")),
            )
        ).andExpect { status { isBadRequest() } }
        reviewRequest(
            listOf(decision(SUSPECT, SuspiciousCandidateDecision.BAN, emptyList()))
        ).andExpect { status { isBadRequest() } }
        reviewRequest(
            listOf(decision(SUSPECT, SuspiciousCandidateDecision.BAN, listOf("loader"), "x".repeat(256)))
        ).andExpect { status { isBadRequest() } }

        assertEquals(CandidateStatus.PENDING to null, candidate("loader", SUSPECT))
        assertEquals(CandidateStatus.PENDING to null, candidate("runtime", SUSPECT))
        assertEquals(0, bannedCount())
    }

    private fun pending(query: String): List<SuspiciousCandidateResponse> =
        mockMvc.get("/suspicious-candidates$query")
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
            .let { objectMapper.readValue(it, Array<SuspiciousCandidateResponse>::class.java).toList() }

    private fun decision(
        groupId: String,
        decision: SuspiciousCandidateDecision,
        artifacts: List<String>,
        note: String = "Reviewed by human",
    ) = ReviewSuspiciousCandidateDecision(60001, groupId, decision, artifacts, note)

    private fun review(decisions: List<ReviewSuspiciousCandidateDecision>): List<SuspiciousCandidateReviewResult> =
        reviewRequest(decisions)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
            .let { objectMapper.readValue(it, Array<SuspiciousCandidateReviewResult>::class.java).toList() }

    private fun reviewRequest(decisions: List<ReviewSuspiciousCandidateDecision>) =
        post("review", ReviewSuspiciousCandidatesRequest(decisions))

    private fun reviewJson(body: String): List<SuspiciousCandidateReviewResult> =
        postJson("review", body)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
            .let { objectMapper.readValue(it, Array<SuspiciousCandidateReviewResult>::class.java).toList() }

    private fun post(action: String, body: Any) =
        mockMvc.post("/suspicious-candidates/$action") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }

    private fun postJson(action: String, body: String) =
        mockMvc.post("/suspicious-candidates/$action") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    private fun candidate(artifactId: String, groupId: String): Pair<CandidateStatus, String?> =
        candidateRepository.findById(SuspiciousPackageCandidateKey(60001, artifactId, groupId)).get()
            .let { it.status to it.notes }

    private fun bannedArtifacts(): List<String?> =
        jdbcTemplate.queryForList(
            "SELECT artifact_id FROM banned_packages WHERE group_id = ?", String::class.java, SUSPECT
        )

    private fun bannedCount(): Int = bannedArtifacts().size
}
