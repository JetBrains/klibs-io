package io.klibs.core.pckg

import SmokeTestBase
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.api.PackageOverviewResponse
import io.klibs.core.pckg.api.PackageTargetResponse
import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.get
import kotlin.collections.map
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageControllerTest : SmokeTestBase() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return target groups mapping`() {
        // Act & Assert
        val result = mockMvc.get("/package/target-groups")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val targetGroupsMapping: Map<String, List<String>> = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<Map<String, List<String>>>() {}
        )

        // Verify that we have all target groups
        assertEquals(TargetGroup.entries.size, targetGroupsMapping.size, "Expected all target groups in the response")

        // Verify that all target groups are present
        TargetGroup.entries.forEach { targetGroup ->
            assertTrue(
                targetGroupsMapping.containsKey(targetGroup.name),
                "Target group ${targetGroup.name} should be present"
            )

            // Get the targets for this target group from the response
            val targetsInResponse = targetGroupsMapping[targetGroup.name]
            assertNotNull(targetsInResponse, "${targetGroup.name} targets should be present")

            // Verify that all expected targets for this target group are present in the response
            targetGroup.targets.forEach { target ->
                assertTrue(targetsInResponse.contains(target), "${targetGroup.name} targets should contain $target")
            }

            // Verify that the response contains exactly the expected targets for this target group
            assertEquals(targetGroup.targets, targetsInResponse, "${targetGroup.name} targets should match exactly")
        }
    }

    @Test
    @Sql(scripts = ["classpath:sql/PackageIndexTest/seed-project-with-packages.sql"])
    @Sql(
        scripts = ["classpath:sql/PackageIndexTest/seed-project-with-packages-cleanup.sql"],
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    fun `should return latest packages by group ID`() {
        val groupId = "org.example"

        val result = mockMvc.get("/package/$groupId/artifacts")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response: List<PackageOverviewResponse> = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<List<PackageOverviewResponse>>() {}
        )

        assertTrue(response.isNotEmpty(), "Should return at least one package")

        val IGNORED = 0L
        val expected = listOf(
            PackageOverviewResponse(
                id = 19003,
                groupId = "org.example",
                artifactId = "libA",
                version = "2.0.0",
                releasedAtMillis = IGNORED, // ignored
                description = "New A",
                targets = listOf(
                    PackageTargetResponse(platform = "jvm", target = "1.8"),
                    PackageTargetResponse(platform = "js", target = null),
                ),
            ),
            PackageOverviewResponse(
                id = 19004,
                groupId = "org.example",
                artifactId = "libB",
                version = "3.1.4",
                releasedAtMillis = IGNORED, // ignored
                description = "Lib B",
                targets = listOf(
                    PackageTargetResponse(platform = "js", target = null),
                ),
            ),
        )

        val normalizedActual = response
            .map { it.copy(releasedAtMillis = IGNORED) }
            .sortedBy { it.id }

        val normalizedExpected = expected
            .sortedBy { it.id }

        assertEquals(normalizedExpected, normalizedActual, "Response should match expected packages (excluding releasedAtMillis)")
    }
}
