package io.klibs.core.readme

import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.core.storage.S3StorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class S3ReadmeCRUDServiceTest {

    private lateinit var readmeProperties: ReadmeConfigurationProperties
    private lateinit var s3StorageService: S3StorageService
    private lateinit var uut: S3ReadmeCRUDService

    @BeforeEach
    fun setUp() {
        readmeProperties = ReadmeConfigurationProperties(
            cacheDir = File("build/cache/readme"),
            s3 = ReadmeConfigurationProperties.S3Properties(
                bucketName = "test-bucket",
                prefix = "readme"
            )
        )
        s3StorageService = mock()
        uut = S3ReadmeCRUDService(readmeProperties, s3StorageService)
    }

    @Test
    fun `readReadmeMdByAnyId returns content when project object exists`() {
        val projectId = 456
        val scmRepositoryId = 123
        val key = "readme/project/readme-456.md"
        val content = "README content"

        whenever(s3StorageService.readText("test-bucket", key)).thenReturn(content)

        val result = uut.readReadmeMd(projectId, scmRepositoryId)

        assertEquals(content, result)
    }

    @Test
    fun `readReadmeMdByAnyId falls back to repo key when project key is missing`() {
        val projectId = 456
        val scmRepositoryId = 123
        val projectKey = "readme/project/readme-456.md"
        val repoKey = "readme/readme-123.md"
        val content = "README content"

        whenever(s3StorageService.readText("test-bucket", projectKey)).thenReturn(null)
        whenever(s3StorageService.readText("test-bucket", repoKey)).thenReturn(content)

        val result = uut.readReadmeMd(projectId, scmRepositoryId)

        assertEquals(content, result)
    }

    @Test
    fun `writeReadmeFilesByProjectId uploads raw, md and html files`() {
        val projectId = 456
        val rawContent = "raw content"
        val mdContent = "MD content"
        val htmlContent = "HTML content"

        uut.writeReadmeFiles(projectId, rawContent, mdContent, htmlContent)

        verify(s3StorageService).writeText("test-bucket", "readme/project/readme-456-raw.md", rawContent)
        verify(s3StorageService).writeText("test-bucket", "readme/project/readme-456.md", mdContent)
        verify(s3StorageService).writeText("test-bucket", "readme/project/readme-456.html", htmlContent)
    }
}
