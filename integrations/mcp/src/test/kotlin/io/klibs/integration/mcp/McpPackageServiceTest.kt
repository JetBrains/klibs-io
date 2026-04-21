package io.klibs.integration.mcp

import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.service.PackageService
import io.klibs.integration.mcp.service.McpPackageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class McpPackageServiceTest {

    private val packageService = mock(PackageService::class.java)
    private val mcpPackageService = McpPackageService(packageService)

    @Test
    fun `getLatestVersion returns both latest and latest stable versions when package exists`() {
        val groupId = "org.jetbrains.kotlinx"
        val artifactId = "kotlinx-coroutines-core"
        val expectedLatestVersion = "1.9.0-RC"
        val expectedStableVersion = "1.8.0"

        whenever(packageService.getLatestPackageDetails(groupId, artifactId))
            .thenReturn(createPackageDetails(groupId, artifactId, expectedLatestVersion))

        whenever(packageService.getLatestStablePackageDetails(groupId, artifactId))
            .thenReturn(createPackageDetails(groupId, artifactId, expectedStableVersion))

        val result = mcpPackageService.getLatestVersion(groupId, artifactId)

        assertTrue(result.packageFound)
        assertEquals(groupId, result.groupId)
        assertEquals(artifactId, result.artifactId)
        assertEquals(expectedLatestVersion, result.latestVersion?.version)
        assertEquals(expectedStableVersion, result.latestStableVersion?.version)
    }

    @Test
    fun `getLatestVersion returns latest version with null stable version when no stable version exists`() {
        val groupId = "org.jetbrains.kotlinx"
        val artifactId = "kotlinx-coroutines-core"
        val expectedLatestVersion = "1.9.0-RC"

        whenever(packageService.getLatestPackageDetails(groupId, artifactId))
            .thenReturn(createPackageDetails(groupId, artifactId, expectedLatestVersion))

        whenever(packageService.getLatestStablePackageDetails(groupId, artifactId))
            .thenReturn(null)

        val result = mcpPackageService.getLatestVersion(groupId, artifactId)

        assertTrue(result.packageFound)
        assertEquals(groupId, result.groupId)
        assertEquals(artifactId, result.artifactId)
        assertEquals(expectedLatestVersion, result.latestVersion?.version)
        assertNull(result.latestStableVersion)
    }

    @Test
    fun `getLatestVersion returns same version for latest and stable when latest is stable`() {
        val groupId = "org.jetbrains.kotlinx"
        val artifactId = "kotlinx-coroutines-core"
        val expectedVersion = "1.8.0"

        whenever(packageService.getLatestPackageDetails(groupId, artifactId))
            .thenReturn(createPackageDetails(groupId, artifactId, expectedVersion))

        whenever(packageService.getLatestStablePackageDetails(groupId, artifactId))
            .thenReturn(createPackageDetails(groupId, artifactId, expectedVersion))

        val result = mcpPackageService.getLatestVersion(groupId, artifactId)

        assertTrue(result.packageFound)
        assertEquals(expectedVersion, result.latestVersion?.version)
        assertEquals(expectedVersion, result.latestStableVersion?.version)
    }

    @Test
    fun `getLatestVersion returns not found when package does not exist`() {
        val groupId = "com.nonexistent"
        val artifactId = "nonexistent-lib"

        whenever(packageService.getLatestPackageDetails(groupId, artifactId))
            .thenReturn(null)

        val result = mcpPackageService.getLatestVersion(groupId, artifactId)

        assertFalse(result.packageFound)
        assertEquals(groupId, result.groupId)
        assertEquals(artifactId, result.artifactId)
        assertNull(result.latestVersion)
        assertNull(result.latestStableVersion)
    }

    private fun createPackageDetails(groupId: String, artifactId: String, version: String) =
        PackageDetails(
            id = 1L,
            projectId = 1,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            releasedAt = Instant.now(),
            description = "Test package",
            targets = emptyList(),
            licenses = emptyList(),
            developers = emptyList(),
            buildTool = "Gradle",
            buildToolVersion = "8.0",
            kotlinVersion = "2.0.0",
            url = null,
            scmUrl = null
        )
}
