package io.klibs.core.project

import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.project.repository.MarkerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.service.ReadmeServiceDispatcher
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectServiceTest {

    private val packageService: PackageService = mock()
    private val readmeServiceDispatcher: ReadmeServiceDispatcher = mock()
    private val projectRepository: ProjectRepository = mock()
    private val packageRepository: PackageRepository = mock()
    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val markerRepository: MarkerRepository = mock()
    private val tagRepository: TagRepository = mock()
    private val projectTagRepository: ProjectTagRepository = mock()
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository = mock()
    private val project: ProjectEntity = mock()

    private val uut = ProjectService(
        packageService,
        readmeServiceDispatcher,
        projectRepository,
        packageRepository,
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

        assertEquals(emptyList<String>(), result)
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

        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("valid-tag")).thenReturn("Valid Tag")
        whenever(allowedProjectTagsRepository.findCanonicalNameByValue("another-tag")).thenReturn("Another Tag")

        val result = uut.updateProjectTags(projectName, ownerLogin, tags, tagsType)

        assertEquals(listOf("Valid Tag", "Another Tag"), result)
        verify(projectTagRepository).deleteByProjectIdAndOrigin(projectId, tagsType)
        verify(projectTagRepository).saveAll(any<List<TagEntity>>())
    }
}
