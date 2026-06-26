package io.klibs.integration.maven.utils

import io.klibs.integration.maven.dto.GavCoordinatesDTO
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GavCoordinatesDTOUtilsValidateTest {

    @Test
    fun `should return null for correct input`() {
        val parsed = GavCoordinatesDTO(
            groupId = "org.jetbrains.kotlinx_test",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )
        val error = MavenArtifactDTOUtils.validateGAVField(parsed)

        assertNull(error)
    }

    @Test
    fun `should return error when group id contains invalid characters`() {
        val parsed = GavCoordinatesDTO(
            groupId = "org.jetbrains/kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )
        val error = MavenArtifactDTOUtils.validateGAVField(parsed)

        assertNotNull(error)
        assertEquals(
            "Invalid Group ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed.",
            error
        )
    }

    @Test
    fun `should return error when artifact id contains spaces`() {
        val parsed = GavCoordinatesDTO(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx coroutines core",
            version = "1.10.2"
        )
        val error = MavenArtifactDTOUtils.validateGAVField(parsed)

        assertNotNull(error)
        assertEquals(
            "Invalid Artifact ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed.",
            error
        )
    }

    @Test
    fun `should allow special characters in version`() {
        val parsed = GavCoordinatesDTO(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2-revision._$~=+:"
        )
        val error = MavenArtifactDTOUtils.validateGAVField(parsed)

        assertNull(error)
    }

    @Test
    fun `should allow null version for indexing all versions`() {
        val parsed = GavCoordinatesDTO(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = null
        )
        val error = MavenArtifactDTOUtils.validateGAVField(parsed)

        assertNull(error)
    }

}