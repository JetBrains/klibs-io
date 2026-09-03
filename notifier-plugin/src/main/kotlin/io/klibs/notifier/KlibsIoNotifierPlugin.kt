package io.klibs.notifier

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Registers the `notifyKlibsIo` task that notifies klibs.io about a published KMP artifact.
 *
 * The task supports two modes:
 * - Automatic: it is wired as a finalizer of the publishing task and runs only when that task has
 *   completed successfully. If the publishing task fails (or is not executed), it is skipped.
 * - Explicit: when invoked directly (e.g. `./gradlew notifyKlibsIo`) it runs regardless of whether
 *   a publishing task ran, so it can be triggered on demand.
 *
 * Note: with the vanniktech plugin, the actual upload to Maven Central happens in the plugin's own
 * build service at the very end of the build — after notifyKlibsIo has already run. If that upload
 * fails, the notification has already been sent, so it becomes a false positive.
 * (see: `com.vanniktech.maven.publish.central.MavenCentralBuildService.close()`)
 */
class KlibsIoNotifierPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("klibsIoNotifier", KlibsIoNotifierExtension::class.java)
        extension.apiUrl.convention("https://klibs.io/notify/artifacts")
        extension.publishTaskName.convention("publishKotlinMultiplatformPublicationToMavenCentralRepository")

        project.afterEvaluate {
            if (!plugins.hasPlugin(MavenPublishPlugin::class.java) ||
                !plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
            ) {
                logger.warn(
                    "klibs-io-notifier is inactive: it needs the Kotlin Multiplatform plugin and a publishing " +
                        "plugin based on Gradle's maven-publish (such as com.vanniktech.maven.publish). " +
                        "No notification will be sent."
                )
            }
        }

        project.plugins.withType(MavenPublishPlugin::class.java) {
            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {

                val notifyTask = project.tasks.register("notifyKlibsIo", KlibsIoNotifierTask::class.java) {
                    apiUrl.convention(extension.apiUrl)
                    publishTaskScheduled.convention(false)
                    group = "publishing"
                    description = "Notifies klibs.io about artifacts published to Maven Central."
                }

                val publishing = project.extensions.getByType(PublishingExtension::class.java)
                val publication = project.provider {
                    val publications = publishing.publications.filterIsInstance<MavenPublication>()
                    publications.firstOrNull { it.name == "kotlinMultiplatform" }
                }

                val tracker = project.gradle.sharedServices.registerIfAbsent(
                    "klibsIoNotifierPublishTracker${project.path}",
                    KlibsIoPublishTracker::class.java,
                )

                val publicationState =
                    publication.map { kmpPublication ->
                        val hasToolingMetadata = kmpPublication.artifacts.any {
                            it.classifier == "kotlin-tooling-metadata" && it.extension == "json"
                        }
                        if (hasToolingMetadata) KmpPublicationState.OK else KmpPublicationState.MISSING_TOOLING_METADATA
                    }.orElse(KmpPublicationState.MISSING_PUBLICATION)

                val publishTaskName = extension.publishTaskName.get()

                project.tasks.matching { it.name == publishTaskName }.configureEach {
                    usesService(tracker)
                    doLast { tracker.get().markPublished() }
                    finalizedBy(notifyTask)
                }

                project.gradle.taskGraph.whenReady {
                    val isPublishScheduled = allTasks.any { it.name == publishTaskName }
                    notifyTask.get().publishTaskScheduled.set(isPublishScheduled)
                }

                notifyTask.configure {
                    artifact.convention(publication.map {
                        ArtifactCoordinates(it.groupId, it.artifactId, it.version)
                    })

                    usesService(tracker)

                    onlyIf("runs when called directly or after a successful publish") {
                        !publishTaskScheduled.get() || tracker.get().published
                    }
                    onlyIf("the KMP publication with kotlin-tooling-metadata.json exists") { task ->
                        when (publicationState.get()) {
                            KmpPublicationState.OK -> true
                            KmpPublicationState.MISSING_PUBLICATION -> {
                                task.logger.warn(
                                    "Cannot notify klibs.io: no 'kotlinMultiplatform' publication found"
                                )
                                false
                            }

                            KmpPublicationState.MISSING_TOOLING_METADATA -> {
                                task.logger.warn(
                                    "Cannot notify klibs.io: the kotlin-tooling-metadata.json artifact is missing"
                                )
                                false
                            }
                        }
                    }
                }
            }
        }
    }
}
