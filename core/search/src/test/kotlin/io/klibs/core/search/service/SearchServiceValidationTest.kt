package io.klibs.core.search.service

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKERS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_OWNER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_QUERY_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAGS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAG_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TARGET_GROUP_FILTERS
import io.klibs.core.search.repository.PackageSearchRepository
import io.klibs.core.search.repository.ProjectSearchRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

@SpringJUnitConfig(SearchServiceValidationTest.Config::class)
class SearchServiceValidationTest {

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var projectSearchRepository: ProjectSearchRepository

    @Autowired
    private lateinit var packageSearchRepository: PackageSearchRepository

    @BeforeEach
    fun setUp() {
        clearMocks(projectSearchRepository, packageSearchRepository)
        every { projectSearchRepository.find(any(), any(), any(), any(), any(), any(), any(), any()) } returns emptyList()
        every { packageSearchRepository.find(any(), any(), any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `accepts all inputs at their limits`() {
        assertDoesNotThrow {
            searchProjects(
                query = "y".repeat(MAX_QUERY_LENGTH),
                targetGroupFilters = List(MAX_TARGET_GROUP_FILTERS) { mapOf(TargetGroup.JVM to emptySet()) },
                ownerLogin = "o".repeat(MAX_OWNER_LENGTH),
                markers = List(MAX_MARKERS) { "m".repeat(MAX_MARKER_LENGTH) },
                tags = List(MAX_TAGS) { "t".repeat(MAX_TAG_LENGTH) },
            )
        }
        assertDoesNotThrow {
            searchPackages(
                query = "y".repeat(MAX_QUERY_LENGTH),
                targetGroupFilters = List(MAX_TARGET_GROUP_FILTERS) { mapOf(TargetGroup.JVM to emptySet()) },
                ownerLogin = "o".repeat(MAX_OWNER_LENGTH),
            )
        }
        verify(exactly = 1) { projectSearchRepository.find(any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { packageSearchRepository.find(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `rejects a query over the length limit`() {
        assertRejectedProjectSearch { searchProjects(query = "y".repeat(MAX_QUERY_LENGTH + 1)) }
        assertRejectedPackageSearch { searchPackages(query = "y".repeat(MAX_QUERY_LENGTH + 1)) }
    }

    @Test
    fun `rejects an owner login over the length limit`() {
        assertRejectedProjectSearch { searchProjects(ownerLogin = "o".repeat(MAX_OWNER_LENGTH + 1)) }
        assertRejectedPackageSearch { searchPackages(ownerLogin = "o".repeat(MAX_OWNER_LENGTH + 1)) }
    }

    @Test
    fun `rejects too many target group filters`() {
        val filters = List(MAX_TARGET_GROUP_FILTERS + 1) { mapOf(TargetGroup.JVM to emptySet<String>()) }
        assertRejectedProjectSearch { searchProjects(targetGroupFilters = filters) }
        assertRejectedPackageSearch { searchPackages(targetGroupFilters = filters) }
    }

    @Test
    fun `rejects invalid target group filters`() {
        val filters = listOf(mapOf(TargetGroup.Unknown to emptySet<String>()))
        assertRejectedProjectSearch { searchProjects(targetGroupFilters = filters) }
        assertRejectedPackageSearch { searchPackages(targetGroupFilters = filters) }
    }

    @Test
    fun `rejects too many tags`() {
        assertRejectedProjectSearch { searchProjects(tags = List(MAX_TAGS + 1) { "tag$it" }) }
    }

    @Test
    fun `rejects a tag over the length limit`() {
        assertRejectedProjectSearch { searchProjects(tags = listOf("ok", "t".repeat(MAX_TAG_LENGTH + 1))) }
    }

    @Test
    fun `rejects too many markers`() {
        assertRejectedProjectSearch { searchProjects(markers = List(MAX_MARKERS + 1) { "marker$it" }) }
    }

    @Test
    fun `rejects a marker over the length limit`() {
        assertRejectedProjectSearch {
            searchProjects(markers = listOf("GRANT_WINNER_2024", "m".repeat(MAX_MARKER_LENGTH + 1)))
        }
    }

    private fun assertRejectedProjectSearch(block: () -> Unit) {
        assertThrows<ConstraintViolationException> { block() }
        verify(exactly = 0) { projectSearchRepository.find(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    private fun assertRejectedPackageSearch(block: () -> Unit) {
        assertThrows<ConstraintViolationException> { block() }
        verify(exactly = 0) { packageSearchRepository.find(any(), any(), any(), any(), any(), any()) }
    }

    private fun searchProjects(
        query: String? = null,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>> = emptyList(),
        ownerLogin: String? = null,
        markers: List<String> = emptyList(),
        tags: List<String> = emptyList(),
    ) = searchService.search(
        query = query,
        targetGroupFilters = targetGroupFilters,
        ownerLogin = ownerLogin,
        sort = SearchSort.RELEVANCY,
        markers = markers,
        tags = tags,
        page = 1,
        limit = 10
    )

    private fun searchPackages(
        query: String? = null,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>> = emptyList(),
        ownerLogin: String? = null,
    ) = searchService.searchPackage(
        query = query,
        targetGroupFilters = targetGroupFilters,
        ownerLogin = ownerLogin,
        sort = SearchSort.RELEVANCY,
        page = 1,
        limit = 10
    )

    @Configuration
    class Config {
        @Bean
        fun projectSearchRepository(): ProjectSearchRepository = mockk()

        @Bean
        fun packageSearchRepository(): PackageSearchRepository = mockk()

        @Bean
        fun searchService(
            projectSearchRepository: ProjectSearchRepository,
            packageSearchRepository: PackageSearchRepository,
        ) = SearchService(
            projectSearchRepository = projectSearchRepository,
            packageSearchRepository = packageSearchRepository,
            projectIndexJdbc = mockk(),
            packageIndexJdbc = mockk(),
            applicationScope = mockk(),
        )

        companion object {
            @Bean
            @JvmStatic
            fun methodValidationPostProcessor() = MethodValidationPostProcessor()
        }
    }
}
