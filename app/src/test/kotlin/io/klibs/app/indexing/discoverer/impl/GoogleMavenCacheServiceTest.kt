package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.configuration.properties.GoogleMavenCacheConfigurationProperties
import io.klibs.app.service.impl.S3GoogleMavenCacheService
import io.klibs.core.storage.S3StorageService
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GoogleMavenCacheServiceTest {

    @Test
    fun `S3GoogleMavenCacheService should read and write from S3`() {
        val s3StorageService: S3StorageService = mockk()
        val properties = GoogleMavenCacheConfigurationProperties(
            s3 = GoogleMavenCacheConfigurationProperties.S3Properties(
                bucketName = "test-bucket",
                prefix = "gmaven"
            )
        )
        val service = S3GoogleMavenCacheService(properties, s3StorageService)
        val groupId = "com.example"
        val content = "<metadata><androidx.compose.runtime versions=\"1.0.0\"/></metadata>"
        val key = "gmaven/group-index-com-example.xml"

        // Mock download not exists
        every { s3StorageService.readText("test-bucket", key) } returns null
        assertNull(service.readGroupIndexFromCache(groupId))

        // Mock download exists
        every { s3StorageService.readText("test-bucket", key) } returns content
        val cached = service.readGroupIndexFromCache(groupId)
        assertNotNull(cached)
        assertEquals(1, cached.elements.size)
        assertEquals("androidx.compose.runtime", cached.elements[0].name)

        // Test write
        service.writeGroupIndexToCache(groupId, content)
        verify { s3StorageService.writeText("test-bucket", key, content) }
    }
}
