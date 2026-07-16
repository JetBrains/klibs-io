package org.kotlintoolchain.plugin.jib

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.tar.TarExtractor
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JibTest {

    @Test
    fun `separates project output from external dependencies`() {
        val tempDir = Files.createTempDirectory("jib-classpath-test-")
        val projectRoot = tempDir / "project"
        val projectJar = projectRoot / "build" / "tasks" / "_app_jarJvm" / "app-jvm.jar"
        val externalJar = tempDir / "maven-cache" / "dependency.jar"
        val similarPrefixJar = tempDir / "project-cache" / "other.jar"

        val classpath = partitionRuntimeClasspath(
            listOf(externalJar, projectJar, similarPrefixJar),
            projectRoot,
        )

        assertEquals(listOf(externalJar, similarPrefixJar), classpath.externalDependencies)
        assertEquals(listOf(projectJar), classpath.projectDependencies)
    }

    @Test
    fun `applies all configured tags to tar image`() {
        val tempDir = Files.createTempDirectory("jib-tags-test-")
        val outputTar = tempDir / "image.tar"
        val imageName = "registry.example.com/klibs/app"
        val containerizer = Containerizer.to(
            TarImage.at(outputTar).named("$imageName:primary"),
        ).withAdditionalTags(listOf("commit", "latest"))

        Jib.fromScratch().containerize(containerizer)

        val extractedTar = tempDir / "extracted"
        TarExtractor.extract(outputTar, extractedTar)
        val manifest = (extractedTar / "manifest.json").readText()
        listOf("primary", "commit", "latest").forEach { tag ->
            assertTrue(
                "\"$imageName:$tag\"" in manifest,
                "Expected tag '$tag' in image manifest: $manifest",
            )
        }
    }
}
