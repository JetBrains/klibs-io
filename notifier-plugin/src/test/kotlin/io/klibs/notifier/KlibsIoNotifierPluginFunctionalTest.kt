package io.klibs.notifier

import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

class KlibsIoNotifierPluginFunctionalTest {

    private val currentKotlinVersion = System.getProperty("test.kotlinVersion")
        ?: error("test.kotlinVersion system property not set")

    private val currentVanniktechVersion = System.getProperty("test.vanniktechVersion")
        ?: error("test.vanniktechVersion system property not set")

    @TempDir
    lateinit var projectDir: File

    private lateinit var server: HttpServer

    @Volatile
    private var receivedBody: String? = null

    @Volatile
    private var responseCode = 200

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/notify/artifacts") { exchange ->
            receivedBody = exchange.requestBody.readBytes().decodeToString()
            exchange.sendResponseHeaders(responseCode, -1)
            exchange.close()
        }
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    private val disablePublishDependencies = """
        tasks.named("publishToMavenCentral") {
            setDependsOn(emptyList<Any>())
        }
    """.trimIndent()

    private fun writeProject(
        extraBuildScript: String = "",
        groupId: String = "com.example",
        artifactId: String = "test-artifact",
        version: String = "1.2.3",
        apiBaseUrl: String = "http://localhost:${server.address.port}",
        gradleProperties: String = "",
        vanniktechVersion: String = currentVanniktechVersion,
        kotlinVersion: String = currentKotlinVersion,
    ) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            rootProject.name = "test-project"
            """.trimIndent()
        )
        File(projectDir, "gradle.properties").writeText(gradleProperties)
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("multiplatform") version "$kotlinVersion"
                id("com.vanniktech.maven.publish") version "$vanniktechVersion"
                id("io.klibs.notifier")
            }

            kotlin { jvm() }

            mavenPublishing {
                publishToMavenCentral()
                coordinates("${groupId.escaped()}", "${artifactId.escaped()}", "${version.escaped()}")
            }

            klibsIoNotifier {
                apiBaseUrl.set("$apiBaseUrl")
            }

            $extraBuildScript
            """.trimIndent()
        )
    }

    private fun String.escaped(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private fun runner(argument: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(argument)

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `notifies klibs with publication coordinates when publishing succeeds`() {
        writeProject(disablePublishDependencies)

        val result = runner("publishToMavenCentral").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":notifyKlibsIo")?.outcome)
        assertEquals(
            """{"groupId":"com.example","artifactId":"test-artifact","version":"1.2.3"}""",
            receivedBody,
        )
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `notifies klibs on the minimum supported versions`() {
        // The minimal versions come from vanniktech.gradle-maven-publish-plugin
        // https://github.com/vanniktech/gradle-maven-publish-plugin/blob/main/CHANGELOG.md
        writeProject(
            extraBuildScript = disablePublishDependencies,
            vanniktechVersion = "0.36.0",
            kotlinVersion = "2.2.0",
        )

        val result = runner("publishToMavenCentral")
            .withGradleVersion("9.0.0")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":notifyKlibsIo")?.outcome)
        assertEquals(
            """{"groupId":"com.example","artifactId":"test-artifact","version":"1.2.3"}""",
            receivedBody,
        )
    }

    @Test
    @Timeout(120, unit = TimeUnit.SECONDS)
    fun `notifies klibs on the latest published vanniktech version`() {
        val latestVersion = latestVanniktechVersion()
        writeProject(
            extraBuildScript = disablePublishDependencies,
            vanniktechVersion = latestVersion,
        )

        val result = runner("publishToMavenCentral").build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":notifyKlibsIo")?.outcome,
            "klibs-io-notifier is not compatible with the latest vanniktech release $latestVersion",
        )
        assertEquals(
            """{"groupId":"com.example","artifactId":"test-artifact","version":"1.2.3"}""",
            receivedBody,
        )
    }

    private fun latestVanniktechVersion(): String {
        val metadataUrl =
            "https://repo1.maven.org/maven2/com/vanniktech/gradle-maven-publish-plugin/maven-metadata.xml"
        val request = HttpRequest.newBuilder(URI.create(metadataUrl))
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build()
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) {
            "Failed to fetch vanniktech maven-metadata.xml: HTTP ${response.statusCode()}"
        }
        return Regex("<release>(.+?)</release>").find(response.body())?.groupValues?.get(1)
            ?: error("Could not find <release> in vanniktech maven-metadata.xml")
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `notifies klibs on every publish with the configuration cache enabled`() {
        writeProject(
            extraBuildScript = disablePublishDependencies,
            gradleProperties = "org.gradle.configuration-cache=true",
        )
        val expectedBody = """{"groupId":"com.example","artifactId":"test-artifact","version":"1.2.3"}"""

        val firstRun = runner("publishToMavenCentral").build()

        assertEquals(TaskOutcome.SUCCESS, firstRun.task(":notifyKlibsIo")?.outcome)
        assertEquals(expectedBody, receivedBody)

        receivedBody = null
        val secondRun = runner("publishToMavenCentral").build()

        assertTrue(secondRun.output.contains("Reusing configuration cache."))
        assertEquals(TaskOutcome.SUCCESS, secondRun.task(":notifyKlibsIo")?.outcome)
        assertEquals(expectedBody, receivedBody)
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `encodes publication coordinates in the notification body`() {
        writeProject(
            extraBuildScript = disablePublishDependencies,
            version = "1.0.0+\"build\"",
        )

        val result = runner("publishToMavenCentral").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":notifyKlibsIo")?.outcome)
        assertEquals(
            """{"groupId":"com.example","artifactId":"test-artifact","version":"1.0.0+\"build\""}""",
            receivedBody,
        )
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `does not notify klibs when applied to a KMP project without the vanniktech plugin`() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            rootProject.name = "test-project"
            """.trimIndent()
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("multiplatform") version "$currentKotlinVersion"
                id("io.klibs.notifier")
            }

            kotlin { jvm() }
            """.trimIndent()
        )

        val result = runner("help").build()

        assertNull(result.task(":notifyKlibsIo"))
        assertNull(receivedBody)
        assertTrue(
            result.output.contains(
                "only supports publishing KMP libraries via com.vanniktech.maven.publish"
            )
        )
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `does not notify klibs when publishing fails`() {
        writeProject(
            """
            val failPublish by tasks.registering {
                doLast { error("simulated publish failure") }
            }
            tasks.named("publishToMavenCentral") {
                setDependsOn(listOf(failPublish))
            }
            """.trimIndent()
        )

        val result = runner("publishToMavenCentral").buildAndFail()

        assertNull(result.task(":notifyKlibsIo"))
        assertNull(receivedBody)
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `does not notify klibs when the tooling metadata artifact is disabled`() {
        // The disable flag was deprecated in 2.3.20 and removed in KGP 2.4.0 (the artifact is always published since),
        // so this guard only exists for older KGP versions
        writeProject(
            extraBuildScript = disablePublishDependencies,
            gradleProperties = "kotlin.mpp.enableKotlinToolingMetadataArtifact=false",
            kotlinVersion = "2.2.0",
        )

        val result = runner("publishToMavenCentral").build()

        assertEquals(TaskOutcome.SKIPPED, result.task(":notifyKlibsIo")?.outcome)
        assertNull(receivedBody)
        assertTrue(
            result.output.contains(
                "Cannot notify klibs.io: the kotlin-tooling-metadata.json artifact is missing"
            )
        )
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `warns without failing the build when the server responds with an error`() {
        responseCode = 500
        writeProject(disablePublishDependencies)

        val result = runner("publishToMavenCentral").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":notifyKlibsIo")?.outcome)
        assertTrue(result.output.contains("Failed to notify klibs.io: HTTP 500"))
        assertEquals(
            """{"groupId":"com.example","artifactId":"test-artifact","version":"1.2.3"}""",
            receivedBody,
        )
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `warns without failing the build when klibs is unreachable`() {
        val unreachablePort = ServerSocket(0).use { it.localPort }
        writeProject(
            extraBuildScript = disablePublishDependencies,
            apiBaseUrl = "http://localhost:$unreachablePort",
        )

        val result = runner("publishToMavenCentral").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":notifyKlibsIo")?.outcome)
        assertTrue(result.output.contains("Failed to reach klibs.io at http://localhost:$unreachablePort/notify/artifacts"))
        assertNull(receivedBody)
    }
}
