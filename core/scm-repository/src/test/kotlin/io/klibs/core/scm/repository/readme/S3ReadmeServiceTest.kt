package io.klibs.core.scm.repository.readme

import io.klibs.core.storage.S3StorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class S3ReadmeServiceTest {

    private lateinit var readmeProperties: ReadmeConfigurationProperties
    private lateinit var s3StorageService: S3StorageService
    private lateinit var uut: S3ReadmeService

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
        uut = S3ReadmeService(readmeProperties, s3StorageService)
    }

    @Test
    fun `readReadmeMd returns content when object exists`() {
        val scmRepositoryId = 123
        val key = "readme/readme-123.md"
        val content = "README content"
        
        whenever(s3StorageService.readText("test-bucket", key)).thenReturn(content)

        val result = uut.readReadmeMd(scmRepositoryId)

        assertEquals(content, result)
    }

    @Test
    fun `readReadmeMd returns null when object does not exist`() {
        val scmRepositoryId = 123
        val key = "readme/readme-123.md"
        
        whenever(s3StorageService.readText("test-bucket", key)).thenReturn(null)

        val result = uut.readReadmeMd(scmRepositoryId)

        assertNull(result)
    }

    @Test
    fun `writeReadmeFiles uploads both md and html files`() {
        val scmRepositoryId = 123
        val mdContent = "MD content"
        val htmlContent = "HTML content"
        
        uut.writeReadmeFiles(scmRepositoryId, mdContent, htmlContent)

        verify(s3StorageService).writeText("test-bucket", "readme/readme-123.md", mdContent)
        verify(s3StorageService).writeText("test-bucket", "readme/readme-123.html", htmlContent)
    }
}
