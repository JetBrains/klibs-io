package io.klibs.core.pckg

import SmokeTestBase
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
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
            assertTrue(targetGroupsMapping.containsKey(targetGroup.name), "Target group ${targetGroup.name} should be present")

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
}
