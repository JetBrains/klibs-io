package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.MavenPom
import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.PomWithReleaseDate
import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.androidx.ModuleMetadataWrapper
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateImpl
import io.klibs.integration.maven.dto.MavenMetadata
import io.klibs.integration.maven.exception.MavenRateLimitedException
import io.klibs.integration.maven.request.RequestRateLimiter
import io.klibs.integration.maven.search.MavenSearchClient
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.parseOrNull
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.search.api.transport.Java11HttpClientTransport
import org.apache.maven.search.api.transport.Transport
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.KotlinToolingMetadataParsingResult
import org.jetbrains.kotlin.tooling.parseJson
import org.slf4j.Logger

private const val DEFAULT_PAGE_SIZE = 200
internal const val MAX_REDIRECTS = 3
private const val HTTP_TOO_MANY_REQUESTS = 429 // not in HttpURLConnection constants

abstract class BaseMavenSearchClient(
    private val xmlMapper: XmlMapper,
    protected val rateLimiter: RequestRateLimiter,
    private val logger: Logger,
    private val objectMapper: ObjectMapper,
    protected val clientTransport: Transport = Java11HttpClientTransport(),
    private val clock: Clock = Clock.System,
    private val lastModifiedHeader: String,
) : MavenSearchClient, MavenStaticDataProvider {

    private val mavenXpp3Reader = MavenXpp3Reader()

    override fun pageSize(): Int = DEFAULT_PAGE_SIZE

    override fun getPom(mavenArtifact: MavenArtifact): MavenPom? {
        return getPomWithReleaseDate(mavenArtifact)?.pom
    }

    override fun getPomWithReleaseDate(mavenArtifact: MavenArtifact): PomWithReleaseDate? {
        val pomFileUrl = getPomUrl(mavenArtifact)
        return executeFetch(pomFileUrl) { response ->
            val pom =
                mavenXpp3Reader.read(StringReader(response.body.readAllBytes().toString(StandardCharsets.UTF_8)))
            PomWithReleaseDate(pom, getReleasedAt(response).toJavaInstant())
        }
    }

    override fun getMavenMetadata(groupId: String, artifactId: String): MavenMetadata? {
        val fileDir = groupId.replace(".", "/") + "/$artifactId"
        val metadataUrl = "${getContentUrlPrefix()}$fileDir/maven-metadata.xml"
        return executeFetch(metadataUrl) { response ->
            xmlMapper.readValue(response.body, MavenMetadata::class.java)
        }
    }

    override fun getKotlinToolingMetadata(mavenArtifact: MavenArtifact): KotlinToolingMetadataDelegate? {
        val kotlinToolingMetadataUrl = getRemoteFileUrl(
            groupId = mavenArtifact.groupId,
            artifactId = mavenArtifact.artifactId,
            version = mavenArtifact.version,
            fileName = "-kotlin-tooling-metadata.json"
        )


        return executeFetch(kotlinToolingMetadataUrl) { response ->
            when (val parseResult =
                KotlinToolingMetadata.parseJson(
                    String(
                        response.body.readAllBytes(),
                        StandardCharsets.UTF_8
                    )
                )) {
                is KotlinToolingMetadataParsingResult.Failure -> throw IllegalArgumentException(parseResult.reason)
                is KotlinToolingMetadataParsingResult.Success -> KotlinToolingMetadataDelegateImpl(validate(parseResult.value))
            }
        }
    }

    override fun getPomUrl(mavenArtifact: MavenArtifact): String {
        return getRemoteFileUrl(
            groupId = mavenArtifact.groupId,
            artifactId = mavenArtifact.artifactId,
            version = mavenArtifact.version,
            fileName = ".pom"
        )
    }

    override fun getModuleMetadata(
        groupId: String,
        artifactId: String,
        version: String
    ): ModuleMetadataWrapper? {
        val metadataUri = getRemoteFileUrl(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            fileName = ".module"
        )
        return executeFetch(metadataUri) { response ->
            val body = response.body ?: throw IllegalStateException("Missing gradle metadata body")
            val gradleMetadata = objectMapper.readValue(body, GradleMetadata::class.java)

            ModuleMetadataWrapper(gradleMetadata = gradleMetadata, releasedAt = getReleasedAt(response).toJavaInstant())
        }
    }

    protected abstract fun getContentUrlPrefix(): String

    /**
     * URL prefix to retry content fetches against when [getContentUrlPrefix] returns 404. Returning `null`
     * (the default) disables the fallback.
     *
     * Used to bypass intermediaries (e.g. cache-redirector) that may return spurious 404s for newly published
     * artifacts that exist on the upstream origin.
     */
    protected open fun getContentFallbackUrlPrefix(): String? = null

    protected fun <T> executeWithThrottle(body: () -> T): T {
        try {
            return rateLimiter.withRateLimitBlocking {
                body.invoke()
            }
        } catch (e: IOException) {
            logger.error("Unsuccessful transport request", e)
            throw IllegalStateException(e)
        }
    }

    private fun <R> executeFetch(
        serviceUri: String,
        headers: Map<String, String> = emptyMap(),
        converter: (response: Transport.Response) -> R,
    ): R? {
        val primary = followRedirects(
            serviceUri = serviceUri,
            headers = headers,
            converter = converter,
            redirectCount = 0,
            requestExecutor = clientTransport::get
        )
        if (primary != null) return primary

        val primaryPrefix = getContentUrlPrefix()
        val fallbackPrefix = getContentFallbackUrlPrefix() ?: return null
        if (fallbackPrefix == primaryPrefix || !serviceUri.startsWith(primaryPrefix)) return null

        val fallbackUri = fallbackPrefix + serviceUri.removePrefix(primaryPrefix)
        logger.warn("Primary content endpoint returned 404 for {}, retrying via {}", serviceUri, fallbackUri)
        return followRedirects(
            serviceUri = fallbackUri,
            headers = headers,
            converter = converter,
            redirectCount = 0,
            requestExecutor = clientTransport::get
        )
    }

    private fun <R> followRedirects(
        serviceUri: String,
        headers: Map<String, String>,
        converter: (response: Transport.Response) -> R,
        redirectCount: Int,
        requestExecutor: (String, Map<String, String>) -> Transport.Response
    ): R? {
        return executeWithThrottle {
            requestExecutor.invoke(serviceUri, headers).use { response ->
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> converter.invoke(response)
                    HttpURLConnection.HTTP_NOT_FOUND -> null
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307, // HTTP_TEMP_REDIRECT (not in HttpURLConnection constants)
                    308  // HTTP_PERM_REDIRECT (not in HttpURLConnection constants)
                        -> {
                        val location = requireNotNull(response.headers["location"]) {
                            "Location of a moved resource cannot be null"
                        }
                        logger.trace("Redirecting to $location")
                        if (redirectCount + 1 > MAX_REDIRECTS) {
                            throw IOException("Too many redirects when fetching $serviceUri -> $location")
                        }
                        followRedirects(location, headers, converter, redirectCount + 1, requestExecutor)
                    }

                    HTTP_TOO_MANY_REQUESTS -> {
                        applyRequestCooldown(response, serviceUri)
                    }

                    else -> throw IOException("Unexpected response: ${response.code}")
                }
            }
        }
    }

    private fun applyRequestCooldown(response: Transport.Response, serviceUri: String): Nothing {
        logger.warn("Rate limited by Maven Central, retrying after ${response.headers["Retry-After"]}")
        val retryAfter = parseRetryAfter(response)
        retryAfter?.let { rateLimiter.applyCooldown(it) }
        throw MavenRateLimitedException.forUrl(serviceUri, retryAfter)
    }

    private fun validate(metadata: KotlinToolingMetadata): KotlinToolingMetadata {
        require(!metadata.projectSettings.isKPMEnabled) { // hardcoded to false in KGP
            "isKPMEnabled is no longer hardcoded to false, changes needed"
        }
        require(metadata.schemaVersion == "1.0.0" || metadata.schemaVersion == "1.1.0") {
            "Schema version has changed, changes needed"
        }
        return metadata
    }

    internal fun getRemoteFileUrl(
        groupId: String,
        artifactId: String,
        version: String,
        fileName: String
    ): String {
        require(fileName.startsWith("-") || fileName.startsWith(".")) {
            "fileName must begin with - or ."
        }

        // Use URLEncoder to mitigate potential malicious special characters
        fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

        val encodedGroupId = groupId.split(".").joinToString("/") { it.urlEncode() }
        val encodedArtifactId = artifactId.urlEncode()
        val encodedVersion = version.urlEncode()
        val encodedFileName = fileName.urlEncode()

        val fileDir = "$encodedGroupId/$encodedArtifactId/$encodedVersion"
        val fullFileName = "$encodedArtifactId-$encodedVersion$encodedFileName"

        return "${getContentUrlPrefix()}$fileDir/$fullFileName"
    }


    private fun parseRetryAfter(response: Transport.Response): Instant? {
        val value = response.headers.entries
            .firstOrNull { it.key.equals("retry-after", ignoreCase = true) }
            ?.value
            ?.trim()
            ?: return null
        val now = clock.now()

        value.toLongOrNull()?.let { seconds ->
            return if (seconds > 0) now.plus(seconds.seconds) else null
        }

        val retryAfter = try {
            parseRfc1123Instant(value)
        } catch (_: Exception) {
            return null
        }

        return retryAfter.takeIf { it > now }
    }

    protected open fun getReleasedAt(response: Transport.Response): Instant {
        val lastModified = response.getHeaderValue(lastModifiedHeader)
            ?: throw IllegalStateException("Missing release date header: expected $lastModifiedHeader")

        return try {
            parseRfc1123Instant(lastModified)
        } catch (e: Exception) {
            throw IllegalStateException("Invalid release date format: $lastModified", e)
        }
    }

    protected fun parseRfc1123Instant(value: String): Instant {

        return Instant.parseOrNull(value, DateTimeComponents.Formats.RFC_1123)
            ?: Instant.fromEpochMilliseconds(value.toLong())
    }

    protected fun Transport.Response.getHeaderValue(headerName: String): String? {
        return headers.entries.firstOrNull { it.key.equals(headerName, ignoreCase = true) }?.value
    }
}