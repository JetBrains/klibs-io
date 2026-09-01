package io.klibs.notifier

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@UntrackedTask(because = "Sends a notification to klibs.io on every successful publish")
abstract class KlibsIoNotifierTask : DefaultTask() {

    @get:Input
    abstract val apiBaseUrl: Property<String>

    @get:Input
    @get:Optional
    internal abstract val artifact: Property<ArtifactCoordinates>

    @TaskAction
    fun notifyKlibs() {
        try {
            sendNotification()
        } catch (e: Exception) {
            this.logger.warn("Failed to notify klibs.io: $e")
        }
    }

    private fun sendNotification() {
        val coordinates = artifact.get()
        val url = "${apiBaseUrl.get()}/notify/artifacts"
        val body = JsonOutput.toJson(
            mapOf(
                "groupId" to coordinates.groupId,
                "artifactId" to coordinates.artifactId,
                "version" to coordinates.version,
            )
        )

        this.logger.lifecycle("Notifying klibs.io: POST $url")

        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
            .build()

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        val response = try {
            client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        } catch (e: Exception) {
            this.logger.warn("Failed to reach klibs.io at $url: $e")
            return
        }

        if (response.statusCode() !in 200 until 300) {
            this.logger.warn("Failed to notify klibs.io: HTTP ${response.statusCode()}\n${response.body()}")
        }else {
            this.logger.lifecycle("Successfully notified klibs.io: HTTP ${response.statusCode()}\n${response.body()}")
        }
    }
}
