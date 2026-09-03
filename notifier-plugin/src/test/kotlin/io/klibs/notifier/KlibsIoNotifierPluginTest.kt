package io.klibs.notifier

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KlibsIoNotifierPluginTest {

    @Test
    fun `registers the klibsIoNotify extension`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("io.klibs.notifier")

        assertNotNull(project.extensions.findByName("klibsIoNotifier"))
    }

    @Test
    fun `registers the notify task if the maven publish and KMP plugins present`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("io.klibs.notifier")
        project.plugins.apply("com.vanniktech.maven.publish")
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")

        assertNotNull(project.tasks.findByName("notifyKlibsIo"))
    }

    @Test
    fun `collects coordinates only of KMP maven publications`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("io.klibs.notifier")
        project.plugins.apply("com.vanniktech.maven.publish")
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("kotlinMultiplatformImpostor", MavenPublication::class.java) {
            groupId = "com.example"
            artifactId = "first-artifact"
            version = "1.0.0"
        }
        (publishing.publications.getByName("kotlinMultiplatform") as MavenPublication).apply {
            groupId = "com.other"
            artifactId = "second-artifact"
            version = "2.0.0"
        }

        val task = project.tasks.getByName("notifyKlibsIo") as KlibsIoNotifierTask

        assertEquals(
            ArtifactCoordinates("com.other", "second-artifact", "2.0.0"),
            task.artifact.get(),
        )
    }

    @Test
    fun `does not register the notify task if the KMP plugin not present`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("io.klibs.notifier")
        project.plugins.apply("com.vanniktech.maven.publish")

        assertNull(project.tasks.findByName("notifyKlibsIo"))
    }

    @Test
    fun `does not register the notify task if the maven publish plugin not present`() {
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("io.klibs.notifier")
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")

        assertNull(project.tasks.findByName("notifyKlibsIo"))
    }
}
