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
import io.klibs.core.readme.service.ReadmeService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectServiceTest {

    private val packageService: PackageService = mock()
    private val readmeService: ReadmeService = mock()
    private val projectRepository: ProjectRepository = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val markerRepository: MarkerRepository = mock()
    private val tagRepository: TagRepository = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository = mock()
    private val project: ProjectEntity = mock()

    private val uut = ProjectService(
        packageService,
        readmeService,
        projectRepository,
        scmRepositoryRepository,
        markerRepository,
        tagRepository,
        projectTagRepository,
        allowedProjectTagsRepository
    )

    @Test
    fun `updateProjectTags deletes GitHub tags when normalized tags are empty`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("", " ", "!!!")
        val tagsType = TagOrigin.GITHUB

        whenever(projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin)).thenReturn(project)
        whenever(project.idNotNull).thenReturn(projectId)

        val result = uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)

        assertEquals(emptyList(), result)
        verify(projectTagRepository).deleteByProjectIdAndOrigin(projectId, tagsType)
        verify(projectTagRepository, never()).saveAll(any<List<TagEntity>>())
    }

    @Test
    fun `updateProjectTags throws exception for USER tags when normalized tags are empty`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("", " ", "!!!")
        val tagsType = TagOrigin.USER

        whenever(projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin)).thenReturn(project)
        whenever(project.idNotNull).thenReturn(projectId)

        assertFailsWith<IllegalArgumentException> {
            uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)
        }
        verify(projectTagRepository, never()).deleteByProjectIdAndOrigin(any(), any())
    }

    @Test
    fun `updateProjectTags saves GitHub tags when they pass normalization and are allowed`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("valid-tag", "another-tag")
        val tagsType = TagOrigin.GITHUB

        whenever(projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin)).thenReturn(project)
        whenever(project.idNotNull).thenReturn(projectId)

        whenever(allowedProjectTagsRepository.existsById("valid-tag")).thenReturn(true)
        whenever(allowedProjectTagsRepository.existsById("another-tag")).thenReturn(true)

        val result = uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)

        assertEquals(listOf("valid-tag", "another-tag"), result)
        verify(projectTagRepository).deleteByProjectIdAndOrigin(projectId, tagsType)
        verify(projectTagRepository).saveAll(any<List<TagEntity>>())
    }

    @Test
    fun `updateProjectTags throws exception for USER tags when tags are not allowed`() {
        val projectName = "test-project"
        val ownerLogin = "test-owner"
        val projectId = 1
        val tags = listOf("invalid-tag")
        val tagsType = TagOrigin.USER

        whenever(projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin)).thenReturn(project)
        whenever(project.idNotNull).thenReturn(projectId)
        whenever(allowedProjectTagsRepository.existsById("invalid-tag")).thenReturn(false)

        val exception = assertFailsWith<IllegalArgumentException> {
            uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)
        }
        assertEquals("Invalid tags were provided. After normalization they are: invalid-tag", exception.message)
        verify(projectTagRepository, never()).deleteByProjectIdAndOrigin(any(), any())
    }

}
