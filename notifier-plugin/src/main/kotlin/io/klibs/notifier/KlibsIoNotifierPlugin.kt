package io.klibs.notifier

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

/**
 * Registers the `notifyKlibs` task that notifies klibs.io about a freshly published KMP artifact.
 *
 * The task is not meant to be invoked directly: it is wired as a finalizer of the publishing task
 * and runs automatically only when that publishing task has completed successfully. If the task
 * fails (or is not executed), `notifyKlibs` is skipped. For this reason the task is hidden from the
 * task list (no group) and should not be run on its own.
 *
 * Note: with the vanniktech plugin, the actual upload to Maven Central happens in the plugin's own
 * build service at the very end of the build — after notifyKlibsIo has already run. If that upload
 * fails, the notification has already been sent, so it becomes a false positive.
 * (see: `com.vanniktech.maven.publish.central.MavenCentralBuildService.close()`)
 */
class KlibsIoNotifierPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("klibsIoNotifier", KlibsIoNotifierExtension::class.java)
        extension.apiBaseUrl.convention("https://klibs.io")

        project.afterEvaluate {
            if (!plugins.hasPlugin("com.vanniktech.maven.publish") ||
                !plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
            ) {
                logger.warn(
                    "Cannot notify klibs.io: klibs-io-notifier currently only supports publishing KMP libraries via com.vanniktech.maven.publish"
                )
            }
        }

        project.plugins.withId("com.vanniktech.maven.publish") {
            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {

                val notifyTask = project.tasks.register("notifyKlibsIo", KlibsIoNotifierTask::class.java) {
                    apiBaseUrl.convention(extension.apiBaseUrl)
                    group = null
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

                val publishTaskNames = setOf(
                    "publishToMavenCentral",
                    "publishAndReleaseToMavenCentral",
                    "publishKotlinMultiplatformPublicationToMavenCentralRepository",
                )
                project.tasks.matching { it.name in publishTaskNames }.configureEach {
                    usesService(tracker)
                    doLast { tracker.get().markPublished() }
                    finalizedBy(notifyTask)
                }

                notifyTask.configure {
                    artifact.convention(publication.map {
                        ArtifactCoordinates(it.groupId, it.artifactId, it.version)
                    })

                    usesService(tracker)

                    onlyIf("runs after a successful publishing task") {
                        tracker.get().published
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
