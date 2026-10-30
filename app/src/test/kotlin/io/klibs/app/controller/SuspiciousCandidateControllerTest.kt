package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import io.klibs.app.api.BanSuspiciousCandidateRequest
import io.klibs.app.api.KeepSuspiciousCandidateRequest
import io.klibs.app.api.SuspiciousCandidateResponse
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
    fun `keep resolves every pending row of the groupId and bans nothing`() {
        keep(SUSPECT, "same author").andExpect { status { isOk() } }

        assertEquals(CandidateStatus.RESOLVED to "same author", candidate("loader", SUSPECT))
        assertEquals(CandidateStatus.RESOLVED to "same author", candidate("runtime", SUSPECT))
        assertEquals(CandidateStatus.RESOLVED to "kept by reviewer", candidate("reviewed", SUSPECT))
        assertEquals(CandidateStatus.PENDING to null, candidate("loader", "app.cash.zipline"))
        assertEquals(0, bannedCount())
    }

    @Test
    @Sql(SEED)
    fun `keep of a groupId without pending rows is a conflict`() {
        keep("io.github.unknown", "same author").andExpect { status { isConflict() } }
    }

    @Test
    @Sql(SEED)
    fun `ban bans one artifact and resolves only its row`() {
        ban("runtime", "fork").andExpect { status { isOk() } }

        assertEquals(CandidateStatus.RESOLVED to "fork", candidate("runtime", SUSPECT))
        assertEquals(CandidateStatus.PENDING to null, candidate("loader", SUSPECT))
        assertEquals(listOf("runtime"), bannedArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `ban of an already resolved row is a conflict and bans nothing`() {
        ban("reviewed", "fork").andExpect { status { isConflict() } }

        assertEquals(CandidateStatus.RESOLVED to "kept by reviewer", candidate("reviewed", SUSPECT))
        assertEquals(0, bannedCount())
    }

    @Test
    @Sql(SEED)
    fun `ban without an artifactId is rejected`() {
        post("ban", mapOf("projectId" to 60001, "groupId" to SUSPECT, "note" to "fork"))
            .andExpect { status { isBadRequest() } }

        assertEquals(0, bannedCount())
    }

    @Test
    @Sql(SEED)
    fun `blank or too long notes are rejected`() {
        keep(SUSPECT, " ").andExpect { status { isBadRequest() } }
        ban("runtime", "x".repeat(256)).andExpect { status { isBadRequest() } }

        assertEquals(CandidateStatus.PENDING to null, candidate("runtime", SUSPECT))
        assertEquals(0, bannedCount())
    }

    private fun pending(query: String): List<SuspiciousCandidateResponse> =
        mockMvc.get("/suspicious-candidates$query")
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
            .let { objectMapper.readValue(it, Array<SuspiciousCandidateResponse>::class.java).toList() }

    private fun keep(groupId: String, note: String) =
        post("keep", KeepSuspiciousCandidateRequest(60001, groupId, note))

    private fun ban(artifactId: String, note: String) =
        post("ban", BanSuspiciousCandidateRequest(60001, SUSPECT, artifactId, note))

    private fun post(action: String, body: Any) =
        mockMvc.post("/suspicious-candidates/$action") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
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
