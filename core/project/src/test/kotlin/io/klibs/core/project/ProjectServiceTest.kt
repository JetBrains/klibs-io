package io.klibs.core.project

import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.project.repository.MarkerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.core.project.repository.ProjectHiddenRepository
import io.klibs.core.readme.service.ReadmeService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectServiceTest {

    private val packageService = mockk<PackageService>()
    private val readmeService = mockk<ReadmeService>()
    private val projectRepository = mockk<ProjectRepository>()
    private val scmRepositoryRepository = mockk<ScmRepositoryRepository>()
    private val markerRepository = mockk<MarkerRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val projectTagRepository = mockk<ProjectTagRepository>(relaxed = true)
    private val allowedProjectTagsRepository = mockk<AllowedProjectTagsRepository>()
    private val scmRepoHealthComponentsRepository = mockk<ScmRepoHealthComponentsRepository>()
    private val projectHiddenRepository = mockk<ProjectHiddenRepository>()
    private val project = mockk<ProjectEntity>()

    private val uut = ProjectService(
        packageService,
        readmeService,
        projectRepository,
        scmRepositoryRepository,
        markerRepository,
        tagRepository,
        projectTagRepository,
        allowedProjectTagsRepository,
        scmRepoHealthComponentsRepository,
        projectHiddenRepository,
    )

    @Test
    fun `updateProjectTags deletes GitHub tags when normalized tags are empty`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("", " ", "!!!")
        val tagsType = TagOrigin.GITHUB

        every { projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin) } returns project
        every { project.idNotNull } returns projectId

        val result = uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)

        assertEquals(emptyList(), result)
        verify(exactly = 1) { projectTagRepository.deleteByProjectIdAndOrigin(projectId, tagsType) }
        verify(exactly = 0) { projectTagRepository.saveAll(any<List<TagEntity>>()) }
    }

    @Test
    fun `updateProjectTags throws exception for USER tags when normalized tags are empty`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("", " ", "!!!")
        val tagsType = TagOrigin.USER

        every { projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin) } returns project
        every { project.idNotNull } returns projectId

        assertFailsWith<IllegalArgumentException> {
            uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)
        }
        verify(exactly = 0) { projectTagRepository.deleteByProjectIdAndOrigin(any(), any()) }
    }

    @Test
    fun `updateProjectTags saves GitHub tags when they pass normalization and are allowed`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("valid-tag", "another-tag")
        val tagsType = TagOrigin.GITHUB

        every { projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin) } returns project
        every { project.idNotNull } returns projectId

        every { allowedProjectTagsRepository.existsById("valid-tag") } returns true
        every { allowedProjectTagsRepository.existsById("another-tag") } returns true

        val result = uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)

        assertEquals(listOf("valid-tag", "another-tag"), result)
        verify(exactly = 1) { projectTagRepository.deleteByProjectIdAndOrigin(projectId, tagsType) }
        verify(exactly = 1) { projectTagRepository.saveAll(any<List<TagEntity>>()) }
    }

    @Test
    fun `updateProjectTags throws exception for USER tags when tags are not allowed`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("invalid-tag")
        val tagsType = TagOrigin.USER

        every { projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin) } returns project
        every { project.idNotNull } returns projectId
        every { allowedProjectTagsRepository.existsById("invalid-tag") } returns false

        val exception = assertFailsWith<IllegalArgumentException> {
            uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)
        }
        assertEquals("Invalid tags were provided. After normalization they are: invalid-tag", exception.message)
        verify(exactly = 0) { projectTagRepository.deleteByProjectIdAndOrigin(any(), any()) }
    }

}
