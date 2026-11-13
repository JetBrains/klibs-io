package io.klibs.app.indexing

import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.core.readme.service.ReadmeService.ProjectInfo
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReadmeServiceTest {

    private lateinit var s3ReadmeService: S3ReadmeCRUDService
    private lateinit var readmeMetadataRepository: ReadmeMetadataRepository
    private lateinit var uut: ReadmeService

    @BeforeEach
    fun setUp() {
        s3ReadmeService = mockk(relaxUnitFun = true)
        readmeMetadataRepository = mockk(relaxed = true)
        uut = ReadmeService(s3ReadmeService, readmeMetadataRepository)
    }

    @Test
    fun `readReadmeMd delegates to S3`() {
        val expected = "readme content"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2)
        every { s3ReadmeService.readReadmeMd(1, 2) } returns expected

        val result = uut.readReadmeMd(projectInfo)

        assertEquals(expected, result)
    }

    @Test
    fun `readReadmeHtml delegates to S3`() {
        val expected = "<html>readme</html>"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2)
        every { s3ReadmeService.readReadmeHtml(1, 2) } returns expected

        val result = uut.readReadmeHtml(projectInfo)

        assertEquals(expected, result)
    }

    @Test
    fun `readReadmeRaw delegates to S3`() {
        val expected = "readme content"
        val projectInfo = ProjectInfo(id = 1, scmRepositoryId = 2)
        every { s3ReadmeService.readReadmeRaw(1, 2) } returns expected

        val result = uut.readReadmeRaw(projectInfo)

        assertEquals(expected, result)
    }

    @Test
    fun `writeReadmeFiles delegates to S3`() {
        every { readmeMetadataRepository.save(any()) } returnsArgument 0

        uut.saveReadmeMetadata(1)
        uut.writeReadmeFiles(1, "raw content", "md content", "html content")

        verify(exactly = 1) { s3ReadmeService.writeReadmeFiles(1, "raw content", "md content", "html content") }
        verify(exactly = 1) { readmeMetadataRepository.save(any()) }
    }
}
