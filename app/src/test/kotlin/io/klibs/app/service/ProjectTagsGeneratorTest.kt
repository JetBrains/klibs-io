package io.klibs.app.service

import BaseUnitWithDbLayerTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.klibs.app.service.impl.ProjectTagsGenerationService
import io.klibs.core.project.service.TagService
import io.klibs.integration.ai.AiModelsProperties
import io.klibs.integration.ai.AiService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals

class ProjectTagsGeneratorTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var tagService: TagService

    private val aiModels = AiModelsProperties()

    private fun newGenerator(mockAi: AiService): TagsGenerationService {
        val tagsPrompt = ClassPathResource("ai/prompts/project-tags.md")
        return ProjectTagsGenerationService(
            tagsPrompt = tagsPrompt,
            tagService = tagService,
            aiService = mockAi,
            objectMapper = jacksonObjectMapper(),
            aiModels = aiModels
        )
    }

    @Test
    fun `throws when no description repo description and readme are provided`() {
        val ai = mockk<AiService>()
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
        val ai = mockk<AiService>()
        val generator = newGenerator(ai)

        every {
            ai.executeOpenAiRequest(any(), "generateProjectTags", aiModels.default)
        } returns "{\"indices\":[0,2,5]}"

        val result = generator.generateTagsForProject(
            projectName = "TestProject",
            projectDescription = "Some description mentioning nothing specific",
            repoDescription = "Test repo",
            readmeMdContent = "Minimal README"
        )

        assertEquals(listOf("aes", "agents", "analytics"), result)
    }
}