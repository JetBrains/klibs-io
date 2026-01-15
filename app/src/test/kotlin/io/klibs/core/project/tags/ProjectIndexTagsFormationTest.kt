package io.klibs.core.project.tags

import BaseUnitWithDbLayerTest
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.search.SearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals

@ActiveProfiles("test")
class ProjectIndexTagsFormationTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var tagRepository: TagRepository

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql(
        value = [
            "classpath:sql/ProjectIndexTagsFormationTest/tag_origin_preference.sql"
        ]
    )
    fun `project_index tags should prefer user over github over AI`() {
        // P10001: user + github + AI -> expect only user tags
        val tags10001 = tagRepository.getTagsByProjectId(10001)
        assertEquals(setOf("user-tag-1", "user-tag-2"), tags10001.toSet(), "Expected user-origin tags to be chosen for project 10001")

        // P10002: github + AI (no user) -> expect github tags
        val tags10002 = tagRepository.getTagsByProjectId(10002)
        assertEquals(setOf("gh-tag-2", "gh-tag-3"), tags10002.toSet(), "Expected github-origin tags to be chosen for project 10002")

        // P10003: AI only -> expect AI tags
        val tags10003 = tagRepository.getTagsByProjectId(10003)
        assertEquals(setOf("ai-tag-3", "ai-tag-4"), tags10003.toSet(), "Expected AI-origin tags to be chosen for project 10003")
    }
}
