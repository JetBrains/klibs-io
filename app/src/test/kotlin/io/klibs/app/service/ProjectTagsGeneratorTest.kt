package io.klibs.app.service

import BaseUnitWithDbLayerTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.klibs.app.service.impl.ProjectTagsGenerationService
import io.klibs.core.project.service.TagService
import io.klibs.integration.ai.AiService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals

class ProjectTagsGeneratorTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var tagService: TagService

    private fun newGenerator(mockAi: AiService): TagsGenerationService {
        val tagsPrompt = ClassPathResource("ai/prompts/project-tags.md")
        return ProjectTagsGenerationService(
            tagsPrompt = tagsPrompt,
            tagService = tagService,
            aiService = mockAi,
            objectMapper = jacksonObjectMapper()
        )
    }

    @Test
    fun `throws when no description repo description and readme are provided`() {
        val ai: AiService = mock()
        val generator = newGenerator(ai)

        assertThrows<IllegalStateException> {
            generator.generateTagsForProject(
                projectName = "sample",
                projectDescription = "",
                repoDescription = "",
                readmeMdContent = ""
            )
        }
    }

    @Test
    fun `maps returned indices to allowed tag names`() {
        val ai: AiService = mock()
        val generator = newGenerator(ai)

        // Mock AI response: indices 0, 2, 5 from tag_rules.yaml
        // 0 -> aes, 2 -> agents, 5 -> analytics
        whenever(
            ai.executeOpenAiRequest(
                any(),
                eq("generateProjectTags"),
                eq(AiService.Companion.DEFAULT_GPT)
            )
        ).thenReturn("{" + "\"indices\":[0,2,5]}")

        val result = generator.generateTagsForProject(
            projectName = "TestProject",
            projectDescription = "Some description mentioning nothing specific",
            repoDescription = "Test repo",
            readmeMdContent = "Minimal README"
        )

        assertEquals(listOf("aes", "agents", "analytics"), result)
    }
}