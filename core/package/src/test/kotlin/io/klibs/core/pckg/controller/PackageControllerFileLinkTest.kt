package io.klibs.core.pckg.controller

import io.klibs.core.pckg.model.PackageDetails
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class PackageControllerFileLinkTest {

    private fun details(g: String, a: String, v: String) = PackageDetails(
        id = 0,
        projectId = null,
        groupId = g,
        artifactId = a,
        version = v,
        releasedAt = Instant.EPOCH,
        name = null,
        description = null,
        targets = emptyList(),
        licenses = emptyList(),
        developers = emptyList(),
        buildTool = "",
        buildToolVersion = "",
        kotlinVersion = "",
        url = null,
        scmUrl = null
    )

    @Test
    fun `should build link to google repository`() {
        val url = details("androidx.annotation", "annotation", "1.9.1").getFilesUrl()
        assertEquals(
            "https://maven.google.com/web/index.html#androidx.annotation:annotation:1.9.1",
            url
        )
    }

    @Test
    fun `should build link to maven repository`() {
        val url = details("org.jetbrains.compose.web", "web-svg", "1.3.1").getFilesUrl()
        assertEquals(
            "https://repo1.maven.org/maven2/org/jetbrains/compose/web/web-svg/1.3.1/",
            url
        )
    }

    @Test
    fun `should build link to maven repository when androidx in the name`() {
        val url = details("io.github.edricchan03.androidx.common", "common-enums", "0.3.0").getFilesUrl()
        assertEquals(
            "https://repo1.maven.org/maven2/io/github/edricchan03/androidx/common/common-enums/0.3.0/",
            url
        )
    }


}