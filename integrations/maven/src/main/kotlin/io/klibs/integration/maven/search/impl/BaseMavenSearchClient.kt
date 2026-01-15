package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.MavenPom
import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.androidx.ModuleMetadataWrapper
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateImpl
import io.klibs.integration.maven.request.RequestRateLimiter
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.MavenSearchClient
import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.search.api.MAVEN
import org.apache.maven.search.api.Record
import org.apache.maven.search.api.SearchRequest
import org.apache.maven.search.api.request.Paging
import org.apache.maven.search.api.request.Query
import org.apache.maven.search.api.transport.Java11HttpClientTransport
import org.apache.maven.search.api.transport.Transport
import org.apache.maven.search.backend.smo.SmoSearchBackend
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.DEFAULT_BACKEND_ID
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.DEFAULT_REPOSITORY_ID
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.KotlinToolingMetadataParsingResult
import org.jetbrains.kotlin.tooling.parseJson
import org.slf4j.Logger
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val MAVEN_CENTRAL_REPOSITORY_URL = "https://search.maven.org/remotecontent?filepath="
private const val DEFAULT_PAGE_SIZE = 200
internal const val MAX_REDIRECTS = 3

abstract class BaseMavenSearchClient(
    protected val baseUrl: String,
    private val rateLimiter: RequestRateLimiter,
    private val logger: Logger,
    private val objectMapper: ObjectMapper,
    protected val clientTransport: Transport = Java11HttpClientTransport()
) : MavenSearchClient, MavenStaticDataProvider {

    private val mavenXpp3Reader = MavenXpp3Reader()

    private val searchClient: SmoSearchBackend = createSearchBackend()

    protected open fun createSearchBackend(): SmoSearchBackend = SmoSearchBackendFactory.create(
        DEFAULT_BACKEND_ID,
        DEFAULT_REPOSITORY_ID,
        "$baseUrl/solrsearch/select",
        clientTransport
    )

    override fun pageSize(): Int = DEFAULT_PAGE_SIZE

    override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse {
        val paging = Paging(DEFAULT_PAGE_SIZE, page)
        val request = SearchRequest(paging, query)
        request.nextPage()

        val response = executeWithThrottle {
            rateLimiter.withRateLimitBlocking {
                searchClient.search(request)
            }
        }

        return MavenSearchResponse(
            totalHits = response.totalHits,
            currentHits = response.currentHits,
            page = response.page.map { it.toArtifactData() },
        )
    }

    override fun getPom(mavenArtifact: MavenArtifact): MavenPom? {
        val pomFileUrl = getPomUrl(mavenArtifact)
        return executeFetch(pomFileUrl) { response ->
            mavenXpp3Reader.read(StringReader(response.body.readAllBytes().toString(StandardCharsets.UTF_8)))
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

            ModuleMetadataWrapper(gradleMetadata = gradleMetadata, releasedAt = getReleasedAt(response))
        }
    }

    protected open fun getContentUrlPrefix(): String {
        return MAVEN_CENTRAL_REPOSITORY_URL
    }

    private fun Record.toArtifactData(): ArtifactData {
        return ArtifactData(
            groupId = this.getValue(MAVEN.GROUP_ID),
            artifactId = this.getValue(MAVEN.ARTIFACT_ID),
            version = this.getValue(MAVEN.VERSION),
            releasedAt = Instant.ofEpochMilli(this.lastUpdated)
        )
    }

    private fun <T> executeWithThrottle(body: () -> T): T {
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
        return executeFetchInternal(serviceUri, headers, converter, 0)
    }

    private fun <R> executeFetchInternal(
        serviceUri: String,
        headers: Map<String, String>,
        converter: (response: Transport.Response) -> R,
        redirectCount: Int
    ): R? {
        return executeWithThrottle {
            clientTransport.get(serviceUri, headers).use { response ->
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
                        if (redirectCount + 1 > MAX_REDIRECTS) {
                            throw IOException("Too many redirects when fetching $serviceUri -> $location")
                        }
                        executeFetchInternal(location, headers, converter, redirectCount + 1)
                    }

                    else -> throw IOException("Unexpected response: ${response.code}")
                }
            }
        }
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

    private fun getRemoteFileUrl(
        groupId: String,
        artifactId: String,
        version: String,
        fileName: String
    ): String {
        require(fileName.startsWith("-") || fileName.startsWith(".")) {
            "fileName must begin with - or ."
        }
        val fileDir = groupId.replace(".", "/") + "/$artifactId/$version"
        val fullFileName = "$artifactId-$version$fileName"
        return "${getContentUrlPrefix()}$fileDir/$fullFileName"
    }


    private fun getReleasedAt(response: Transport.Response): Instant {
        val lastModified = response.headers["last-modified"]
            ?: throw IllegalStateException("Missing last-modified header")
        val releasedAt = try {
            DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModified, Instant::from)
        } catch (e: Exception) {
            throw IllegalStateException("Invalid last-modified date format: $lastModified", e)
        }
        return releasedAt
    }
}