package io.klibs.core.project.repository

import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YamlAllowedProjectTagsRepositoryTest {

    private val repository = YamlAllowedProjectTagsRepository(
        ClassPathResource("ai/prompts/tag_rules.yaml")
    )

    @Test
    fun `findCanonicalNameByValue returns canonical name for tags and tag synonyms`() {
        assertEquals("compose-ui", repository.findCanonicalNameByValue("compose-ui"))
        assertEquals("compose-ui", repository.findCanonicalNameByValue("Compose UI"))
        assertEquals("compose-ui", repository.findCanonicalNameByValue("compose_ui"))
        assertEquals("kotlin-flow", repository.findCanonicalNameByValue("flow"))
    }

    @Test
    fun `findCanonicalNameByValue returns null for unknown or blank values`() {
        assertNull(repository.findCanonicalNameByValue("unknown-tag"))
        assertNull(repository.findCanonicalNameByValue("   "))
    }
}