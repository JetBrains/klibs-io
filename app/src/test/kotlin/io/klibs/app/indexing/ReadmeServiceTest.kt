package io.klibs.app.indexing

import io.klibs.core.readme.AndroidxReadmeProvider
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.core.readme.service.ReadmeService.ProjectInfo
import io.klibs.core.readme.service.S3ReadmeCRUDService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ReadmeServiceTest {

    private lateinit var s3ReadmeService: S3ReadmeCRUDService
    private lateinit var androidxReadmeProvider: AndroidxReadmeProvider
    private lateinit var readmeMetadataRepository: ReadmeMetadataRepository
    private lateinit var uut: ReadmeService

    @BeforeEach
    fun setUp() {
        s3ReadmeService = mock()
        androidxReadmeProvider = mock()
        readmeMetadataRepository = mock()
        uut = ReadmeService(s3ReadmeService, androidxReadmeProvider, readmeMetadataRepository)
    }

    @Test
    fun `readReadmeMd delegates to S3 for non-androidx owner`() {
        val expected = "readme content"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2, name = "ktor", ownerLogin = "jetbrains")
        whenever(s3ReadmeService.readReadmeMd(1, 2)).thenReturn(expected)

        val result = uut.readReadmeMd(projectInfo)

        assertEquals(expected, result)
        verifyNoInteractions(androidxReadmeProvider)
    }

    @Test
    fun `readReadmeMd delegates to AndroidxReadmeProvider for androidx owner`() {
        val expected = "androidx readme"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2, name = "compose", ownerLogin = "androidx")
        whenever(androidxReadmeProvider.resolve("compose", "md")).thenReturn(expected)

        val result = uut.readReadmeMd(projectInfo)

        assertEquals(expected, result)
        verifyNoInteractions(s3ReadmeService)
    }

    @Test
    fun `readReadmeHtml delegates to S3 for non-androidx owner`() {
        val expected = "<html>readme</html>"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2, name = "ktor", ownerLogin = "kotlin")
        whenever(s3ReadmeService.readReadmeHtml(1, 2)).thenReturn(expected)

        val result = uut.readReadmeHtml(projectInfo)

        assertEquals(expected, result)
        verifyNoInteractions(androidxReadmeProvider)
    }

    @Test
    fun `readReadmeHtml delegates to AndroidxReadmeProvider for androidx owner`() {
        val expected = "<html>androidx</html>"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2, name = "compose", ownerLogin = "androidx")
        whenever(androidxReadmeProvider.resolve("compose", "html")).thenReturn(expected)

        val result = uut.readReadmeHtml(projectInfo)

        assertEquals(expected, result)
        verifyNoInteractions(s3ReadmeService)
    }

    @Test
    fun `readReadmeRaw delegates to S3 for non-androidx owner`() {
        val expected = "readme content"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2, name = "ktor", ownerLogin = "jetbrains")
        whenever(s3ReadmeService.readReadmeRaw(1, 2)).thenReturn(expected)

        val result = uut.readReadmeRaw(projectInfo)

        assertEquals(expected, result)
        verifyNoInteractions(androidxReadmeProvider)
    }

    @Test
    fun `readReadmeRaw delegates to AndroidxReadmeProvider for androidx owner`() {
        val expected = "androidx readme"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2, name = "compose", ownerLogin = "androidx")
        whenever(androidxReadmeProvider.resolve("compose", "md")).thenReturn(expected)

        val result = uut.readReadmeRaw(projectInfo)

        assertEquals(expected, result)
        verifyNoInteractions(s3ReadmeService)
    }

    @Test
    fun `writeReadmeFiles always delegates to S3`() {
        uut.writeReadmeFiles(1, 1, null,"raw content","md content", "html content")

        verify(s3ReadmeService).writeReadmeFiles(1, "raw content","md content", "html content")
        verify(readmeMetadataRepository).save(any())
        verifyNoInteractions(androidxReadmeProvider)
    }
}
