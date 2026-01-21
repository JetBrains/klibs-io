package io.klibs.integration.maven.configuration

import org.apache.maven.index.DefaultIndexer
import org.apache.maven.index.DefaultIndexerEngine
import org.apache.maven.index.DefaultQueryCreator
import org.apache.maven.index.DefaultSearchEngine
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.creator.JarFileContentsIndexCreator
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator
import org.apache.maven.index.incremental.DefaultIncrementalHandler
import org.apache.maven.index.updater.DefaultIndexUpdater
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Configuration
class MavenIndexerConfiguration {

    @Bean
    fun indexer(): Indexer = DefaultIndexer(
        DefaultSearchEngine(),
        DefaultIndexerEngine(),
        DefaultQueryCreator()
    )

    @Bean
    fun indexUpdater(): IndexUpdater = DefaultIndexUpdater(
        DefaultIncrementalHandler(),
        emptyList()
    )

    @Bean
    fun indexCreators(): List<IndexCreator> = listOf(
        MinimalArtifactInfoIndexCreator(),
        JarFileContentsIndexCreator()
    )

    @Bean
    fun resourceFetcher(): ResourceFetcher = HttpClientResourceFetcher()
}

class HttpClientResourceFetcher : ResourceFetcher {
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private var uri: URI? = null

    @Throws(IOException::class)
    override fun connect(id: String?, url: String?) {
        uri = URI.create("$url/")
    }

    @Throws(IOException::class)
    override fun disconnect() {
        uri = null
    }

    @Throws(IOException::class, FileNotFoundException::class)
    override fun retrieve(name: String?): InputStream? {
        if(uri == null) throw IOException("Not connected")

        val request =
            HttpRequest.newBuilder().uri(uri!!.resolve(name!!)).GET().build()
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return response.body()
            } else {
                throw IOException("Unexpected response: $response")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException(e)
        }
    }
}